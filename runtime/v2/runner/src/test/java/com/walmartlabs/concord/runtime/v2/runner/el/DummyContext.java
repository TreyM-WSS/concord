package com.walmartlabs.concord.runtime.v2.runner.el;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
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

import com.walmartlabs.concord.runtime.v2.sdk.Compiler;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Execution;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import com.walmartlabs.concord.sdk.ProjectInfo;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class DummyContext implements Context {

    @Override
    public Path workingDirectory() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public UUID processInstanceId() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public Variables variables() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public ProjectInfo projectInfo() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public Execution execution() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public Compiler compiler() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public <T> T eval(Object v, Class<T> type) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public void suspend(String eventName) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String suspendResume(Map<String, Serializable> payload) {
        throw new IllegalStateException("Not implemented");
    }
}
