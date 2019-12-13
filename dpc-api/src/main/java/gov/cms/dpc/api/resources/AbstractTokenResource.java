package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.models.JWTAuthResponse;
import io.dropwizard.jersey.jsr310.OffsetDateTimeParam;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
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
     * If no tokens exists, returns an empty {@link List}
     *
     * @param organizationPrincipal - {@link OrganizationPrincipal} supplied by auth handler
     * @return - {@link List} {@link String} base64 (URL) encoded token
     */
    @GET
    public abstract CollectionResponse<TokenEntity> getOrganizationTokens(OrganizationPrincipal organizationPrincipal);

    /**
     * Create authentication token for {@link org.hl7.fhir.dstu3.model.Organization}.
     * This token is designed to be long-lived and delegatable.
     *
     * @param principal  - {@link OrganizationPrincipal} supplied by auth handler
     * @param label      - {@link Optional} {@link String} to use as token label
     * @param expiration - {@link Optional} {@link OffsetDateTime} to use for token expiration
     * @return - {@link String} base64 (URL) encoded token
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @POST
    public abstract TokenEntity createOrganizationToken(OrganizationPrincipal principal, String label, Optional<OffsetDateTimeParam> expiration);

    @GET
    @Path("/{tokenID}")
    public abstract TokenEntity getOrganizationToken(OrganizationPrincipal principal, @PathParam("tokenID") @NotNull UUID tokenID);

    @POST
    @Path("/auth")
    public abstract JWTAuthResponse authorizeJWT(@NotEmpty(message = "Scope is required") String scope, @NotEmpty(message = "Grant type is required") String grantType, @NotEmpty(message = "Assertion type is required") String clientAssertionType, @NotEmpty(message = "Assertion is required") String jwtBody);

    @DELETE
    @Path("/{tokenID}")
    public abstract Response deleteOrganizationToken(OrganizationPrincipal organizationPrincipal, @NotNull @PathParam("tokenID") UUID tokenID);
}
