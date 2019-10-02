package gov.cms.dpc.api.auth;

import org.hl7.fhir.dstu3.model.Organization;

import java.security.Principal;
import java.util.UUID;

/**
 * Simple wrapper class which ensures that the {@link Organization} resource implements {@link Principal}
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
        return UUID.fromString(this.organization.getIdElement().getIdPart());
    }
}
