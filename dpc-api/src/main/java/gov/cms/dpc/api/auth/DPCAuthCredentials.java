package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorized;
import org.hl7.fhir.dstu3.model.Organization;

/**
 * Wrapper class for passing authentication state between the {@link io.dropwizard.auth.AuthFilter} and the {@link io.dropwizard.auth.Authenticator}
 */
public class DPCAuthCredentials {

    private final String macaroon;
    private final Organization organization;
    private final PathAuthorized pa;
    private final String pathValue;

    public DPCAuthCredentials(String macaroon, Organization organization, PathAuthorized pa, String pathValue) {
        this.macaroon = macaroon;
        this.organization = organization;
        this.pa = pa;
        this.pathValue = pathValue;
    }

    public String getMacaroon() {
        return macaroon;
    }

    public Organization getOrganization() {
        return organization;
    }

    public PathAuthorized getPathAuthorizer() {
        return pa;
    }

    public String getPathValue() {
        return pathValue;
    }
}
