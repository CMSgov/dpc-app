package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import org.hl7.fhir.dstu3.model.Organization;

/**
 * Wrapper class for passing authentication state between the {@link io.dropwizard.auth.AuthFilter} and the {@link io.dropwizard.auth.Authenticator}
 */
public class DPCAuthCredentials {

    private final String macaroon;
    private final Organization organization;
    private final PathAuthorizer pa;
    private final String pathValue;
    private final String apiVersion;

    public DPCAuthCredentials(String macaroon, Organization organization, PathAuthorizer pa, String pathValue) {
        this(macaroon, organization, pa, pathValue, null);
    }

    public DPCAuthCredentials(String macaroon, Organization organization, PathAuthorizer pa, String pathValue, String apiVersion) {
        this.macaroon = macaroon;
        this.organization = organization;
        this.pa = pa;
        this.pathValue = pathValue;
        this.apiVersion = apiVersion;
    }

    public String getMacaroon() {
        return macaroon;
    }

    public Organization getOrganization() {
        return organization;
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
