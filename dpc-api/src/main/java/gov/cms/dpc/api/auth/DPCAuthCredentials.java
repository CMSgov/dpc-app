package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import org.hl7.fhir.dstu3.model.Organization;

public class DPCAuthCredentials {

    private final String macaroon;
    private final Organization organization;
    private final PathAuthorizer pa;
    private final String pathValue;

    public DPCAuthCredentials(String macaroon, Organization organization, PathAuthorizer pa, String pathValue) {
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

    public PathAuthorizer getPathAuthorizer() {
        return pa;
    }

    public String getPathValue() {
        return pathValue;
    }
}
