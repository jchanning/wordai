package com.fistraltech.server.dto;

import com.fistraltech.server.AlgorithmFeatureService;

/**
 * Response body describing the effective runtime policy for an algorithm.
 */
public class AlgorithmPolicyResponse {

    private String id;
    private String name;
    private String description;
    private boolean stateful;
    private boolean enabled;

    public AlgorithmPolicyResponse() {
    }

    public static AlgorithmPolicyResponse from(AlgorithmFeatureService.AlgorithmInfo info) {
        AlgorithmPolicyResponse response = new AlgorithmPolicyResponse();
        response.setId(info.getId());
        response.setName(info.getDisplayName());
        response.setDescription(info.getDescription());
        response.setStateful(info.isStateful());
        response.setEnabled(info.isEnabled());
        return response;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isStateful() {
        return stateful;
    }

    public void setStateful(boolean stateful) {
        this.stateful = stateful;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}