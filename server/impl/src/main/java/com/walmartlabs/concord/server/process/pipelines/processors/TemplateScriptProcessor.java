package com.walmartlabs.concord.server.process.pipelines.processors;

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

import com.walmartlabs.concord.common.CycleChecker;
import com.walmartlabs.concord.server.process.Payload;
import com.walmartlabs.concord.server.process.ProcessException;
import com.walmartlabs.concord.server.process.logs.LogManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Named
@Singleton
public class TemplateScriptProcessor implements PayloadProcessor {

    public static final String REQUEST_DATA_TEMPLATE_FILE_NAME = "_main.js";
    public static final String INPUT_REQUEST_DATA_KEY = "_input";

    private final LogManager logManager;
    private final ScriptEngine scriptEngine;

    @Inject
    public TemplateScriptProcessor(LogManager logManager) {
        this.logManager = logManager;
        this.scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Payload process(Chain chain, Payload payload) {
        Path workspace = payload.getHeader(Payload.WORKSPACE_DIR);

        // process _main.js
        Path scriptPath = workspace.resolve(REQUEST_DATA_TEMPLATE_FILE_NAME);
        if (!Files.exists(scriptPath)) {
            return chain.process(payload);
        }

        UUID instanceId = payload.getInstanceId();

        Map<String, Object> in = payload.getHeader(Payload.REQUEST_DATA_MAP);
        Map<String, Object> out = processScript(instanceId, in, scriptPath);

        CycleChecker.CheckResult result = CycleChecker.check(INPUT_REQUEST_DATA_KEY, out);
        if (result.isHasCycle()) {
            throw new ProcessException(instanceId, "Found cycle in " + REQUEST_DATA_TEMPLATE_FILE_NAME + ": " +
                    result.getNode1() + " <-> " + result.getNode2() );
        }
        payload = payload.putHeader(Payload.REQUEST_DATA_MAP, out);

        return chain.process(payload);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> processScript(UUID instanceId, Map meta, Path templateMeta) {
        Object result;
        try (Reader r = new FileReader(templateMeta.toFile())) {
            Bindings b = scriptEngine.createBindings();
            b.put(INPUT_REQUEST_DATA_KEY, meta != null ? meta : Collections.emptyMap());

            result = scriptEngine.eval(r, b);
            if (!(result instanceof Map)) {
                throw new ProcessException(instanceId, "Invalid template result. Expected a Java Map instance, got " + result);
            }
        } catch (IOException | ScriptException e) {
            logManager.error(instanceId, "Template script execution error", e);
            throw new ProcessException(instanceId, "Template script execution error", e);
        }
        return (Map<String, Object>) result;
    }
}
