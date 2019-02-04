package com.walmartlabs.concord.server.org.project;

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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.walmartlabs.concord.common.validation.ConcordKey;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

@JsonInclude(Include.NON_NULL)
public class ProjectEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    public static ProjectEntry replace(ProjectEntry e, Map<String, RepositoryEntry> repos) {
        return new ProjectEntry(e.id, e.name, e.description, e.orgId, e.orgName, repos,
                e.cfg, e.visibility, e.owner, e.acceptsRawPayload, e.meta);
    }

    private final UUID id;

    @ConcordKey
    // TODO it should be final, but swagger makes it readOnly and provides no way to set the value
    private String name;

    @Size(max = 1024)
    private final String description;

    private final UUID orgId;

    @ConcordKey
    private final String orgName;

    @Valid
    private final Map<String, RepositoryEntry> repositories;

    private final Map<String, Object> cfg;

    private final ProjectVisibility visibility;

    private final ProjectOwner owner;

    private final Boolean acceptsRawPayload;

    private final Map<String, Object> meta;

    public ProjectEntry(String name) {
        this(null, name, null, null, null, null, null, null, null, true, null);
    }

    public ProjectEntry(String name, ProjectVisibility visibility) {
        this(null, name, null, null, null, null, null, visibility, null, true, null);
    }

    public ProjectEntry(String name, Map<String, RepositoryEntry> repositories) {
        this(null, name, null, null, null, repositories, null, null, null, true, null);
    }

    public ProjectEntry(String name, UUID id) {
        this(id, name, null, null, null, null, null, null, null, true, null);
    }

    @JsonCreator
    public ProjectEntry(@JsonProperty("id") UUID id,
                        @JsonProperty("name") String name,
                        @JsonProperty("description") String description,
                        @JsonProperty("orgId") UUID orgId,
                        @JsonProperty("orgName") String orgName,
                        @JsonProperty("repositories") Map<String, RepositoryEntry> repositories,
                        @JsonProperty("cfg") Map<String, Object> cfg,
                        @JsonProperty("visibility") ProjectVisibility visibility,
                        @JsonProperty("owner") ProjectOwner owner,
                        @JsonProperty("acceptsRawPayload") Boolean acceptsRawPayload,
                        @JsonProperty("meta") Map<String, Object> meta) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.orgId = orgId;
        this.orgName = orgName;
        this.repositories = repositories;
        this.cfg = cfg;
        this.visibility = visibility;
        this.owner = owner;
        this.acceptsRawPayload = acceptsRawPayload;
        this.meta = meta;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public Map<String, RepositoryEntry> getRepositories() {
        return repositories;
    }

    public Map<String, Object> getCfg() {
        return cfg;
    }

    public ProjectVisibility getVisibility() {
        return visibility;
    }

    public ProjectOwner getOwner() {
        return owner;
    }

    public Boolean getAcceptsRawPayload() {
        return acceptsRawPayload;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    @Override
    public String toString() {
        return "ProjectEntry{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", orgId=" + orgId +
                ", orgName='" + orgName + '\'' +
                ", repositories=" + repositories +
                ", cfg=" + cfg +
                ", visibility=" + visibility +
                ", owner=" + owner +
                ", acceptsRawPayload=" + acceptsRawPayload +
                ", meta=" + meta +
                '}';
    }
}
