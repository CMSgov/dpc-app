package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.PractitionerRole;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/PractitionerRole")
@FHIR
public abstract class AbstractPractitionerRoleResource {

    protected AbstractPractitionerRoleResource() {
        // Not used
    }

    /**
     * Fetch the {@link PractitionerRole} that matches either an {@link org.hl7.fhir.dstu3.model.Organization}, or a {@link org.hl7.fhir.dstu3.model.Practitioner}
     *
     * @param organizationID - {@link String} optional {@link org.hl7.fhir.dstu3.model.Organization} ID to find {@link PractitionerRole}s for
     * @param providerID     - {@link String} optional {@link org.hl7.fhir.dstu3.model.Practitioner} ID to find {@link PractitionerRole}s for
     * @return -{@link Bundle} of {@link PractitionerRole} matching query
     */
    @GET
    public abstract Bundle getPractitionerRoles(String organizationID, String providerID);

    /**
     * Create a {@link PractitionerRole} that associates a given {@link org.hl7.fhir.dstu3.model.Practitioner} with an {@link org.hl7.fhir.dstu3.model.Organization}
     *
     * @param role - {@link PractitionerRole} to create
     * @return - {@link Response}, returns {@link Response.Status#CREATED} if roles was created. Otherwise, returns an error. {@link PractitionerRole} is returned as the body
     */
    @POST
    public abstract Response submitPractitionerRole(PractitionerRole role);

    /**
     * Fetch a {@link PractitionerRole} by the given {@link UUID} ID
     *
     * @param roleID - {@link UUID} of {@link PractitionerRole} to fetch
     * @return - {@link PractitionerRole}
     */
    @GET
    @Path("/{roleID}")
    public abstract PractitionerRole getPractitionerRole(UUID roleID);

    /**
     * Remove a {@link PractitionerRole}, which un-assigns the {@link org.hl7.fhir.dstu3.model.Practitioner} from the {@link org.hl7.fhir.dstu3.model.Organization}
     *
     * @param roleID - {@link UUID} of {@link PractitionerRole} to remove.
     * @return - {@link Response}
     */
    @DELETE
    @Path("/{roleID}")
    public abstract Response deletePractitionerRole(UUID roleID);
}
