package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.models.CreateTokenRequest;
import gov.cms.dpc.api.models.JWTAuthResponse;
import gov.cms.dpc.common.annotations.NoHtml;
import io.dropwizard.jersey.jsr310.OffsetDateTimeParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Path("/Token")
@Produces(value = MediaType.APPLICATION_JSON)
@Consumes(value = MediaType.APPLICATION_JSON)
public abstract class AbstractTokenResource<P extends Principal> {

    protected AbstractTokenResource() {
        // Not used
    }

    /**
     * Get authentication token for the authenticated organization.
     * If no tokens exist, returns an empty {@link List}
     *
     * @param organizationPrincipal - principal supplied by auth handler
     * @return - {@link List} {@link String} base64 (URL) encoded token
     */
    @GET
    public abstract CollectionResponse<TokenEntity> getOrganizationTokens(P organizationPrincipal);

    /**
     * Create authentication token for the authenticated organization.
     * This token is designed to be long-lived and delegatable.
     *
     * @param principal   - principal supplied by auth handler
     * @param label       - {@link Optional} {@link String} to use as token label
     * @param expiration  - {@link Optional} {@link OffsetDateTime} to use for token expiration
     * @param requestBody - {@link CreateTokenRequest} model representing token resource. Token fields take precedence over query parameters.
     * @return - {@link String} base64 (URL) encoded token
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @POST
    public abstract TokenEntity createOrganizationToken(P principal, @Valid CreateTokenRequest requestBody, @NoHtml String label, Optional<OffsetDateTimeParam> expiration);

    @GET
    @Path("/{tokenID}")
    public abstract TokenEntity getOrganizationToken(P principal, @NotNull UUID tokenID);

    @POST
    @Path("/auth")
    public abstract JWTAuthResponse authorizeJWT(@NoHtml @NotEmpty(message = "Scope is required") String scope,
                                                 @NoHtml @NotEmpty(message = "Grant type is required") String grantType,
                                                 @NoHtml @NotEmpty(message = "Assertion type is required") String clientAssertionType,
                                                 @NoHtml @NotEmpty(message = "Assertion is required") String jwtBody);

    @GET
    @Path("/validate")
    public abstract Response validateJWT(@NoHtml @NotEmpty(message = "Must submit JWT") String jwt);

    @DELETE
    @Path("/{tokenID}")
    public abstract Response deleteOrganizationToken(P organizationPrincipal, @NotNull UUID tokenID);
}
