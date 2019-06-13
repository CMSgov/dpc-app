package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.jersey.params.BooleanParam;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Path("/Organization")
public abstract class AbstractOrganizationResource {

    protected AbstractOrganizationResource() {
        // Not used
    }

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

    /**
     * Create authentication token for {@link Organization}.
     * This token is designed to be long-lived and delegatable.
     * <p>
     * By default, this method will NOT overwrite any tokens which already exist,
     * only by passing the refresh query param will any destructive changes take place.
     *
     * @param organizationID - {@link UUID} organization ID
     * @param refresh        - {@link BooleanParam} {@code true} overwrite any existing tokens. {@code false} do not overwrite any tokens
     * @return - {@link String} base64 (URL) encoded token
     */
    @GET
    @Path("/{organizationID}/token/create")
    public abstract String getOrganizationToken(UUID organizationID, Optional<BooleanParam> refresh);

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
