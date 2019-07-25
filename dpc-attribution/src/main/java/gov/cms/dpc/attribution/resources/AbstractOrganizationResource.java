package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Parameters;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Path("/Organization")
public abstract class AbstractOrganizationResource {

    protected AbstractOrganizationResource() {
        // Not used
    }

    @GET
    @FHIR
    public abstract Bundle searchOrganizations(String identifier, String tokenTag);

    /**
     * Register a {@link Organization} with the API
     * <p>
     * We're currently using a {@link Bundle} resource, which allows us to include both the {@link Organization}
     * as well as any {@link org.hl7.fhir.dstu3.model.Endpoint} resources.
     * <p>
     * The {@link Bundle} is submitted as the Resource portion of the {@link Parameters} object.
     *
     * @param bundle - {@link Bundle} to register with system
     * @return - {@link Response} whether operation succeeded or failed
     */
    @POST
    @Path("/$submit")
    @FHIR
    public abstract Response submitOrganization(Parameters bundle);

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

    /**
     * Get authentication token for {@link Organization}.
     * If no token exists, returns an empty {@link List}
     *
     * @param organizationID - {@link UUID} organization ID
     * @return - {@link List} {@link String} base64 (URL) encoded token
     */
    @GET
    @Path("/{organizationID}/token")
    public abstract List<String> getOrganizationTokens(UUID organizationID);

    /**
     * Create authentication token for {@link Organization}.
     * This token is designed to be long-lived and delegatable.
     *
     * @param organizationID - {@link UUID} organization ID
     * @return - {@link String} base64 (URL) encoded token
     */
    @POST
    @Path("/{organizationID}/token")
    public abstract String createOrganizationToken(@PathParam("organizationID") UUID organizationID);

    /**
     * Verify that the provided token is valid
     *
     * @param organizationID - {@link UUID} organization ID
     * @param token          - {@link String} representation of authorization token (optionally base64 encoded)
     * @return - {@link Response} with status {@link Response.Status#OK} if token is valid. {@link Response.Status#UNAUTHORIZED} if token is not valid
     */
    @GET
    @Path("/{organizationID}/token/verify")
    public abstract Response verifyOrganizationToken(@PathParam("organizationID") UUID organizationID, @NotEmpty @QueryParam("token") String token);
}
