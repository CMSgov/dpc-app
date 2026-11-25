package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;

/**
 * Wrapper class for passing authentication state between the {@link io.dropwizard.auth.AuthFilter} and the {@link io.dropwizard.auth.Authenticator}
 */
public class DPCAuthCredentials {

    private final String macaroon;
    private final String organizationId;
    private final PathAuthorizer pa;
    private final String pathValue;
    private final String apiVersion;

    public DPCAuthCredentials(String macaroon, String organizationId, PathAuthorizer pa, String pathValue) {
        this(macaroon, organizationId, pa, pathValue, null);
    }

    public DPCAuthCredentials(String macaroon, String organizationId, PathAuthorizer pa, String pathValue, String apiVersion) {
        this.macaroon = macaroon;
        this.organizationId = organizationId;
        this.pa = pa;
        this.pathValue = pathValue;
        this.apiVersion = apiVersion;
    }

    public String getMacaroon() {
        return macaroon;
    }

    /**
     * @return the organization ID, or null for a golden macaroon on an admin operation
     */
    public String getOrganizationId() {
        return organizationId;
    }

    public PathAuthorizer getPathAuthorizer() {
        return pa;
    }

    public String getPathValue() {
        return pathValue;
    }

    public String getApiVersion() {
        return apiVersion;
    }
}
