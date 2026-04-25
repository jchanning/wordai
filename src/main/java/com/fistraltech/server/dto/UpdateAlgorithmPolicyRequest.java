package com.fistraltech.server.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for updating an algorithm runtime policy.
 */
public class UpdateAlgorithmPolicyRequest {

    @NotNull
    private Boolean enabled;

    public UpdateAlgorithmPolicyRequest() {
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}