package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;

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
    public abstract Bundle searchAndValidateOrganizations(String tokenTag);

    /**
     * Register a {@link Organization} with the API
     * <p>
     * We're currently using a {@link Bundle} resource, which allows us to include both the {@link Organization}
     * as well as any {@link org.hl7.fhir.dstu3.model.Endpoint} resources
     *
     * @param bundle - {@link Bundle} to register with system
     * @return - {@link Response} whether operation succeeded or failed
     */
    @POST
    @FHIR
    public abstract Response createOrganization(Bundle bundle);

    @GET
    @FHIR
    @Path("/{organizationID}")
    public abstract Organization getOrganization(UUID organizationID);

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
     * @return - {@link Boolean} {@code true} token is valid. {@code false} token is not valid
     */
    @GET
    @Path("/{organizationID}/token/verify")
    public abstract boolean verifyOrganizationToken(@PathParam("organizationID") UUID organizationID, @QueryParam("token") String token);
}
