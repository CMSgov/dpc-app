package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.rest.api.MethodOutcome;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.AdminOperation;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.attribution.client.AttributionClient;
import gov.cms.dpc.common.MDCConstants;
import gov.cms.dpc.common.hibernate.auth.DPCAuthHibernateBundle;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.annotations.r4.FHIRParameter;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.*;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

@Api(value = "Organization", authorizations = @Authorization(value = "access_token"))
@Path("/v3/Organization")
public class OrganizationResource {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationResource.class);

    private final AttributionClient client;
    private final TokenDAO tokenDAO;
    private final PublicKeyDAO keyDAO;

    @Inject
    public OrganizationResource(AttributionClient client, TokenDAO tokenDAO, PublicKeyDAO keyDAO) {
        this.client = client;
        this.tokenDAO = tokenDAO;
        this.keyDAO = keyDAO;
    }

    @POST
    @Path("/$submit")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(hidden = true, value = "Create organization by submitting Bundle")
    @AdminOperation
    public Organization submitOrganization(@FHIRParameter(name = "resource") @NotNull Bundle organizationBundle) {
        validateOrganizationBundle(organizationBundle);

        final MethodOutcome outcome = client.submitOrganization(organizationBundle);
        if (outcome == null || !(outcome.getResource() instanceof Organization createdOrg)) {
            throw new WebApplicationException("Unable to submit Organization", Response.Status.INTERNAL_SERVER_ERROR);
        }

        MDC.put(MDCConstants.ORGANIZATION_ID, createdOrg.getIdElement().getIdPart());
        logger.info("dpcMetric=organizationCreated");
        return createdOrg;
    }

    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Get organization details",
            notes = "FHIR endpoint which returns the Organization resource that is currently registered with the application.",
            authorizations = @Authorization(value = "access_token"))
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "An organization is only allowed to see their own Organization resource")})
    public Bundle orgSearch(@ApiParam(hidden = true) @Auth OrganizationPrincipal organization) {
        final Bundle orgBundle = new Bundle();
        orgBundle.addEntry().setResource(client.getOrganization(organization.getID()));
        orgBundle.setType(Bundle.BundleType.COLLECTION);
        orgBundle.setTotal(orgBundle.getEntry().size());
        return orgBundle;
    }

    @GET
    @Path("/{organizationID}")
    @FHIR
    @Timed
    @ExceptionMetered
    @PathAuthorizer(type = DPCResourceType.Organization, pathParam = "organizationID")
    @ApiOperation(value = "Get organization details by UUID",
            notes = "FHIR endpoint which returns the Organization resource that is currently registered with the application.",
            authorizations = @Authorization(value = "access_token"))
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "An organization is only allowed to see their own Organization resource")})
    public Organization getOrganization(@NotNull @PathParam("organizationID") UUID organizationID) {
        return client.getOrganization(organizationID);
    }

    @DELETE
    @Path("/{organizationID}")
    @FHIR
    @Timed
    @ExceptionMetered
    @AdminOperation
    @UnitOfWork(value = DPCAuthHibernateBundle.BUNDLE_NAME)
    @ApiOperation(value = "Delete Organization",
            notes = "FHIR endpoint which removes the organization currently registered with the application.\n" +
                    "This also removes all associated resources",
            authorizations = @Authorization(value = "access_token"))
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Cannot find organization to remove")})
    public Response deleteOrganization(@NotNull @PathParam("organizationID") UUID organizationID) {
        client.deleteOrganization(organizationID);

        tokenDAO.fetchTokens(organizationID).forEach(tokenDAO::deleteToken);
        keyDAO.fetchPublicKeys(organizationID).forEach(keyDAO::deletePublicKey);

        return Response.ok().build();
    }

    @PUT
    @Path("/{organizationID}")
    @PathAuthorizer(type = DPCResourceType.Organization, pathParam = "organizationID")
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Update organization record",
            notes = "Update specific Organization record.",
            authorizations = @Authorization(value = "access_token"))
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "An organization may update only their own Organization resource"),
            @ApiResponse(code = 404, message = "Unable to find Organization to update"),
            @ApiResponse(code = 422, message = "Provided resource is not a valid FHIR Organization")
    })
    public Organization updateOrganization(@NotNull @PathParam("organizationID") UUID organizationID,
                                           @Valid @Profiled Organization organization) {
        final Organization resource = client.updateOrganization(organizationID, organization);
        if (resource == null) {
            throw new WebApplicationException("Unable to update Organization", Response.Status.INTERNAL_SERVER_ERROR);
        }

        return resource;
    }

    private void validateOrganizationBundle(Bundle organizationBundle) {
        organizationBundle
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType().getPath().equals(DPCResourceType.Organization.getPath()))
                .findAny()
                .orElseThrow(() -> new WebApplicationException("Bundle must include Organization", Response.Status.BAD_REQUEST));
    }
}
