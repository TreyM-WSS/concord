package com.walmartlabs.concord.server.org.project;

import com.google.common.base.Splitter;
import com.walmartlabs.concord.common.ConfigurationUtils;
import com.walmartlabs.concord.server.api.GenericOperationResultResponse;
import com.walmartlabs.concord.server.api.OperationResult;
import com.walmartlabs.concord.server.api.org.OrganizationEntry;
import com.walmartlabs.concord.server.api.org.ResourceAccessEntry;
import com.walmartlabs.concord.server.api.org.ResourceAccessLevel;
import com.walmartlabs.concord.server.api.org.project.EncryptValueResponse;
import com.walmartlabs.concord.server.api.org.project.ProjectEntry;
import com.walmartlabs.concord.server.api.org.project.ProjectOperationResponse;
import com.walmartlabs.concord.server.api.org.project.ProjectResource;
import com.walmartlabs.concord.server.org.OrganizationManager;
import com.walmartlabs.concord.server.org.secret.SecretManager;
import org.sonatype.siesta.Resource;
import org.sonatype.siesta.Validate;
import org.sonatype.siesta.ValidationErrorsException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Named
public class ProjectResourceImpl implements ProjectResource, Resource {

    private final OrganizationManager orgManager;
    private final ProjectDao projectDao;
    private final ProjectManager projectManager;
    private final ProjectAccessManager accessManager;
    private final SecretManager secretManager;

    @Inject
    public ProjectResourceImpl(OrganizationManager orgManager,
                               ProjectDao projectDao,
                               ProjectManager projectManager,
                               ProjectAccessManager accessManager,
                               SecretManager secretManager) {

        this.orgManager = orgManager;
        this.projectDao = projectDao;
        this.projectManager = projectManager;
        this.accessManager = accessManager;
        this.secretManager = secretManager;
    }

    @Override
    @Validate
    public ProjectOperationResponse createOrUpdate(String orgName, ProjectEntry entry) {
        OrganizationEntry org = orgManager.assertAccess(orgName, true);

        UUID projectId = entry.getId();
        if (projectId == null) {
            projectId = projectDao.getId(org.getId(), entry.getName());
        }

        if (projectId != null) {
            projectManager.update(projectId, entry);
            return new ProjectOperationResponse(projectId, OperationResult.UPDATED);
        }

        projectId = projectManager.insert(org.getId(), orgName, entry);
        return new ProjectOperationResponse(projectId, OperationResult.CREATED);
    }

    @Override
    @Validate
    public ProjectEntry get(String orgName, String projectName) {
        OrganizationEntry org = orgManager.assertAccess(orgName, false);

        UUID projectId = projectDao.getId(org.getId(), projectName);
        if (projectId == null) {
            throw new WebApplicationException("Project not found: " + projectName, Status.NOT_FOUND);
        }

        return projectManager.get(projectId);
    }

    @Override
    @Validate
    public List<ProjectEntry> list(String orgName) {
        OrganizationEntry org = orgManager.assertAccess(orgName, false);
        return projectManager.list(org.getId());
    }

    @Override
    @Validate
    public Object getConfiguration(String orgName, String projectName, String path) {
        OrganizationEntry org = orgManager.assertAccess(orgName, false);

        UUID projectId = projectDao.getId(org.getId(), projectName);
        if (projectId == null) {
            throw new WebApplicationException("Project not found: " + projectName, Status.NOT_FOUND);
        }

        // TODO move to ProjectManager
        accessManager.assertProjectAccess(projectId, ResourceAccessLevel.READER, false);

        String[] ps = cfgPath(path);
        Object v = projectDao.getConfigurationValue(projectId, ps);

        if (v == null) {
            throw new WebApplicationException("Value not found: " + path, Status.NOT_FOUND);
        }

        return v;
    }

    @Override
    @Validate
    public GenericOperationResultResponse updateConfiguration(String orgName, String projectName, String path, Object data) {
        OrganizationEntry org = orgManager.assertAccess(orgName, false);

        UUID projectId = projectDao.getId(org.getId(), projectName);
        if (projectId == null) {
            throw new WebApplicationException("Project not found: " + projectName, Status.NOT_FOUND);
        }

        // TODO move to ProjectManager
        accessManager.assertProjectAccess(projectId, ResourceAccessLevel.WRITER, true);

        Map<String, Object> cfg = projectDao.getConfiguration(projectId);
        if (cfg == null) {
            cfg = new HashMap<>();
        }

        String[] ps = cfgPath(path);
        if (ps == null || ps.length < 1) {
            if (!(data instanceof Map)) {
                throw new ValidationErrorsException("Expected a JSON object: " + data);
            }
            cfg = (Map<String, Object>) data;
        } else {
            ConfigurationUtils.set(cfg, data, ps);
        }

        Map<String, Object> newCfg = cfg;
        projectDao.updateCfg(projectId, newCfg);
        return new GenericOperationResultResponse(OperationResult.UPDATED);
    }

    @Override
    @Validate
    public GenericOperationResultResponse updateConfiguration(String orgName, String projectName, Object data) {
        return updateConfiguration(projectName, "/", data);
    }

    @Override
    @Validate
    public GenericOperationResultResponse deleteConfiguration(String orgName, String projectName, String path) {
        OrganizationEntry org = orgManager.assertAccess(orgName, false);

        UUID projectId = projectDao.getId(org.getId(), projectName);
        if (projectId == null) {
            throw new WebApplicationException("Project not found: " + projectName, Status.NOT_FOUND);
        }

        // TODO move to ProjectManager
        accessManager.assertProjectAccess(projectId, ResourceAccessLevel.WRITER, true);

        Map<String, Object> cfg = projectDao.getConfiguration(projectId);
        if (cfg == null) {
            cfg = new HashMap<>();
        }

        String[] ps = cfgPath(path);
        if (ps == null || ps.length == 0) {
            cfg = null;
        } else {
            ConfigurationUtils.delete(cfg, ps);
        }

        Map<String, Object> newCfg = cfg;
        projectDao.updateCfg(projectId, newCfg);
        return new GenericOperationResultResponse(OperationResult.DELETED);
    }

    @Override
    @Validate
    public GenericOperationResultResponse delete(String orgName, String projectName) {
        OrganizationEntry org = orgManager.assertAccess(orgName, false);

        UUID projectId = projectDao.getId(org.getId(), projectName);
        if (projectId == null) {
            throw new WebApplicationException("Project not found: " + projectName, Status.NOT_FOUND);
        }

        projectManager.delete(projectId);
        return new GenericOperationResultResponse(OperationResult.DELETED);
    }

    @Override
    @Validate
    public GenericOperationResultResponse updateAccessLevel(String orgName, String projectName, ResourceAccessEntry entry) {
        OrganizationEntry org = orgManager.assertAccess(orgName, true);

        UUID projectId = projectDao.getId(org.getId(), projectName);
        if (projectId == null) {
            throw new WebApplicationException("Project not found: " + projectName, Status.NOT_FOUND);
        }

        accessManager.updateAccessLevel(projectId, entry.getTeamId(), entry.getLevel());
        return new GenericOperationResultResponse(OperationResult.UPDATED);
    }

    @Override
    @Validate
    public EncryptValueResponse encrypt(String orgName, String projectName, String value) {
        if (value == null) {
            throw new ValidationErrorsException("Value is required");
        }

        OrganizationEntry org = orgManager.assertAccess(orgName, true);

        UUID projectId = projectDao.getId(org.getId(), projectName);
        if (projectId == null) {
            throw new WebApplicationException("Project not found: " + projectName, Status.NOT_FOUND);
        }

        accessManager.assertProjectAccess(projectId, ResourceAccessLevel.READER, true);

        byte[] input = value.getBytes();
        byte[] result = secretManager.encryptData(projectName, input);

        return new EncryptValueResponse(result);
    }

    private static String[] cfgPath(String s) {
        if (s == null) {
            return new String[0];
        }

        List<String> l = Splitter.on("/").omitEmptyStrings().splitToList(s);
        return l.toArray(new String[l.size()]);
    }
}
