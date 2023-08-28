package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.FHIRParameter;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Parameters;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/Organization")
public abstract class AbstractOrganizationResource {

    protected AbstractOrganizationResource() {
        // Not used
    }

    @GET
    @FHIR
    public abstract List<Organization> searchOrganizations(String identifier);

    /**
     * Register a {@link Organization} with the API
     * <p>
     * We're currently using a {@link Bundle} resource, which allows us to include both the {@link Organization}
     * as well as any {@link org.hl7.fhir.dstu3.model.Endpoint} resources.
     * <p>
     * The {@link Bundle} is submitted as the Resource portion of the {@link Parameters} object.
     *
     * @param transactionBundle - {@link Bundle} to register with system, submitted via a {@link Parameters} object.
     * @return - {@link Response} whether operation succeeded or failed
     */
    @POST
    @Path("/$submit")
    @FHIR
    public abstract Response submitOrganization(@FHIRParameter Bundle transactionBundle);

    /**
     * Fetch the {@link Organization} with the given ID
     *
     * @param organizationID - {@link UUID} of organization
     * @return - {@link Organization}
     */
    @GET
    @FHIR
    @Path("/{organizationID}")
    public abstract Organization getOrganization(UUID organizationID);

    /**
     * Update the {@link Organization} with the given ID
     *
     * @param organizationID {@link UUID} of organization
     * @param organization   {@link Organization}
     * @return - {@link Response} whether or not the {@link Organization} was updated
     */
    @PUT
    @FHIR
    @Path("/{organizationID}")
    public abstract Response updateOrganization(UUID organizationID, Organization organization);

    /**
     * Delete the {@link Organization} from the system.
     * <p>
     * Note: This drop ALL resources associated to the given Organization
     *
     * @param organizationID - {@link UUID} of organization
     * @return - {@link Response} whether or not the drop was successful
     */
    @DELETE
    @FHIR
    @Path("/{organizationID}")
    public abstract Response deleteOrganization(UUID organizationID);
}
