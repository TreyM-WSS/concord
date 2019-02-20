package com.walmartlabs.concord.agent.executors.runner;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.google.common.hash.HashCode;
import com.walmartlabs.concord.agent.ExecutionException;
import com.walmartlabs.concord.agent.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProcessPool {

    private static final Logger log = LoggerFactory.getLogger(ProcessPool.class);
    private static final long CLEANUP_PERIOD = 30000;

    private final long maxEntryAge;
    private final int maxEntryCount;
    private final Map<HashCode, Queue<ProcessEntry>> pool = new HashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ProcessPool(long maxEntryAge, int maxEntryCount) {
        this.maxEntryAge = maxEntryAge;
        this.maxEntryCount = maxEntryCount;
        init();
    }

    public void init() {
        Thread t = new Thread(() -> {
            log.info("run -> starting cleanup thread, max entry age {}ms, max entry count {}", maxEntryAge, maxEntryCount);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(CLEANUP_PERIOD);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                maintenance();
            }
        }, "process-pool-cleanup");

        t.start();
    }

    public ProcessEntry take(HashCode hc, ProcessLauncher launcher) throws ExecutionException {
        synchronized (pool) {
            Queue<ProcessEntry> q = pool.computeIfAbsent(hc, k -> new LinkedList<>());

            ProcessEntry entry = q.poll();
            if (entry == null) {
                try {
                    entry = launcher.start();
                } catch (IOException e) {
                    throw new ExecutionException("Error while starting a new process", e);
                }

                log.info("take -> started a new process: {}", entry.procDir);
            } else {
                log.info("take -> using a pre-forked instance: {}", entry.procDir);
            }

            executor.submit(() -> populate(hc, launcher));

            return entry;
        }
    }

    private void populate(HashCode hc, ProcessLauncher launcher) {
        synchronized (pool) {
            // calculate the total number of processes in the pool
            int total = 0;
            for (Map.Entry<HashCode, Queue<ProcessEntry>> e : pool.entrySet()) {
                total += e.getValue().size();
            }

            if (total >= maxEntryCount) {
                // mark the oldest entry for removal
                ProcessEntry oldest = null;
                for (Map.Entry<HashCode, Queue<ProcessEntry>> q : pool.entrySet()) {
                    for (ProcessEntry e : q.getValue()) {
                        if (oldest == null || oldest.timestamp > e.timestamp) {
                            oldest = e;
                        }
                    }
                }

                // it's never null
                assert oldest != null;

                oldest.remove = true;
            }

            // add a new pool entry
            Queue<ProcessEntry> q = pool.computeIfAbsent(hc, k -> new LinkedList<>());
            try {
                q.add(launcher.start());
            } catch (IOException e) {
                log.error("populate -> error while starting a new process", e);
            }
        }
    }

    private void maintenance() {
        List<HashCode> queuesToRemove = new ArrayList<>();
        List<Process> processesToKill = new ArrayList<>();

        long t = System.currentTimeMillis();

        synchronized (pool) {
            pool.forEach((hc, q) -> {
                q.removeIf(e -> {
                    if (e.remove || t - e.timestamp >= maxEntryAge) {
                        processesToKill.add(e.process);
                        return true;
                    }
                    return false;
                });

                if (q.isEmpty()) {
                    queuesToRemove.add(hc);
                }
            });

            for (HashCode hc : queuesToRemove) {
                pool.remove(hc);
            }
        }

        log.debug("maintenance -> removed {} queues", queuesToRemove.size());

        for (Process p : processesToKill) {
            Utils.kill(p);
        }
        log.debug("maintenance -> killed {} processes", processesToKill.size());
    }

    public interface ProcessLauncher {

        ProcessEntry start() throws IOException;
    }

    public static final class ProcessEntry {

        private final long timestamp;
        private final Process process;
        private final Path procDir;

        private boolean remove = false;

        public ProcessEntry(Process process, Path procDir) {
            this.timestamp = System.currentTimeMillis();
            this.process = process;
            this.procDir = procDir;
        }

        public Process getProcess() {
            return process;
        }

        public Path getProcDir() {
            return procDir;
        }
    }
}
