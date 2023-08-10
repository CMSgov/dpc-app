package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.models.CreateTokenRequest;
import gov.cms.dpc.api.models.JWTAuthResponse;
import gov.cms.dpc.common.annotations.NoHtml;
import io.dropwizard.jersey.jsr310.OffsetDateTimeParam;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotEmpty;
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
     * @param principal   - {@link OrganizationPrincipal} supplied by auth handler
     * @param label       - {@link Optional} {@link String} to use as token label
     * @param expiration  - {@link Optional} {@link OffsetDateTime} to use for token expiration
     * @param requestBody - {@link CreateTokenRequest} model representing token resource. Token fields take precedence over query parameters.
     * @return - {@link String} base64 (URL) encoded token
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @POST
    public abstract TokenEntity createOrganizationToken(OrganizationPrincipal principal, @Valid CreateTokenRequest requestBody, @NoHtml String label, Optional<OffsetDateTimeParam> expiration);

    @GET
    @Path("/{tokenID}")
    public abstract TokenEntity getOrganizationToken(OrganizationPrincipal principal, @NotNull UUID tokenID);

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
    public abstract Response deleteOrganizationToken(OrganizationPrincipal organizationPrincipal, @NotNull UUID tokenID);
}
