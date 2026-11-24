package gov.cms.dpc.api.auth.r4;

import org.hl7.fhir.r4.model.Organization;

import java.security.Principal;
import java.util.UUID;

/**
 * R4 Organization principal supplied to v3 resources.
 */
public class OrganizationPrincipal implements Principal {

    private final Organization organization;

    public OrganizationPrincipal(Organization organization) {
        this.organization = organization;
    }

    public Organization getOrganization() {
        return organization;
    }

    @Override
    public String getName() {
        return organization.getName();
    }

    public UUID getID() {
        return UUID.fromString(organization.getIdElement().getIdPart());
    }
}
