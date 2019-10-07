package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.TokenEntity;
import io.dropwizard.jersey.jsr310.OffsetDateTimeParam;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Path("/Token")
@Produces(value = MediaType.APPLICATION_JSON)
@Consumes(value = MediaType.APPLICATION_JSON)
public abstract class AbstractTokenResource {

    protected AbstractTokenResource() {
        // Not used
    }

    /**
     * Get authentication token for {@link org.hl7.fhir.dstu3.model.Organization}.
     * If no token exists, returns an empty {@link List}
     *
     * @param organizationID - {@link UUID} organization ID
     * @return - {@link List} {@link String} base64 (URL) encoded token
     */
    @GET
    @Path("/{organizationID}")
    public abstract List<TokenEntity> getOrganizationTokens(OrganizationPrincipal organizationPrincipal, @NotNull @PathParam("organizationID") UUID organizationID);

    /**
     * Create authentication token for {@link org.hl7.fhir.dstu3.model.Organization}.
     * This token is designed to be long-lived and delegatable.
     *
     * @param organizationID - {@link UUID} organization ID
     * @param label          - {@link Optional} {@link String} to use as token label
     * @return - {@link String} base64 (URL) encoded token
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @POST
    @Path("/{organizationID}")
    public abstract String createOrganizationToken(OrganizationPrincipal principal, @PathParam("organizationID") @NotNull UUID organizationID, String label, Optional<OffsetDateTimeParam> expiration);

    @DELETE
    @Path("/{organizationID}/{tokenID}")
    public abstract Response deleteOrganizationToken(OrganizationPrincipal organizationPrincipal, @NotNull @PathParam("organizationID") UUID organizationID, @NotNull @PathParam("tokenID") UUID tokenID);

    /**
     * Verify that the provided token is valid
     *
     * @param organizationID - {@link UUID} organization ID
     * @param token          - {@link String} representation of authorization token (optionally base64 encoded)
     * @return - {@link Response} with status {@link Response.Status#OK} if token is valid. {@link Response.Status#UNAUTHORIZED} if token is not valid
     */
    @GET
    @Path("/{organizationID}/verify")
    public abstract Response verifyOrganizationToken(OrganizationPrincipal principal, @PathParam("organizationID") UUID organizationID, @NotEmpty @QueryParam("token") String token);
}
