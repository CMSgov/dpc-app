package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.AdminOperation;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.api.resources.AbstractOrganizationResource;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.FHIRParameter;
import gov.cms.dpc.fhir.annotations.Profiled;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.*;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Api(value = "Organization", authorizations = @Authorization(value = "access_token"))
@Path("/v1/Organization")
public class OrganizationResource extends AbstractOrganizationResource {

    private final IGenericClient client;
    private final TokenDAO tokenDAO;
    private final PublicKeyDAO keyDAO;

    private static final Logger logger = LoggerFactory.getLogger(OrganizationResource.class);

    @Inject
    public OrganizationResource(@Named("attribution") IGenericClient client, TokenDAO tokenDAO, PublicKeyDAO keyDAO) {
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
    @Override
    public Organization submitOrganization(@FHIRParameter(name = "resource") @NotNull Bundle organizationBundle) {
        // Validate bundle
        validateOrganizationBundle(organizationBundle);

        final Parameters parameters = new Parameters();
        parameters.addParameter().setName("resource").setResource(organizationBundle);

        Organization createdOrg = this.client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(parameters)
                .returnResourceType(Organization.class)
                .encodedJson()
                .execute();

        logger.info("dpcMetric=organizationCreated,orgId={}", createdOrg.getIdElement().getIdPart());
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
        Bundle orgBundle = new Bundle();

        Organization org = this.client
                .read()
                .resource(Organization.class)
                .withId(organization.getID().toString())
                .encodedJson()
                .execute();

        orgBundle.addEntry().setResource(org);
        orgBundle.setType(Bundle.BundleType.COLLECTION);
        orgBundle.setTotal(orgBundle.getEntry().size());
        return orgBundle;
    }

    @Override
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
        return this.client
                .read()
                .resource(Organization.class)
                .withId(organizationID.toString())
                .encodedJson()
                .execute();
    }

    @DELETE
    @Path("/{organizationID}")
    @FHIR
    @Timed
    @ExceptionMetered
    @AdminOperation
    @UnitOfWork
    @ApiOperation(value = "Delete Organization",
            notes = "FHIR endpoint which removes the organization currently registered with the application.\n" +
                    "This also removes all associated resources",
            authorizations = @Authorization(value = "access_token"))
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Cannot find organization to remove")})
    @Override
    public Response deleteOrganization(@NotNull @PathParam("organizationID") UUID organizationID) {
        // Delete from the attribution service
        this.client
                .delete()
                .resourceById(new IdType("Organization", organizationID.toString()))
                .encodedJson()
                .execute();

        // Delete tokens
        this.tokenDAO
                .fetchTokens(organizationID)
                .forEach(this.tokenDAO::deleteToken);

        // Delete public keys
        this.keyDAO
                .fetchPublicKeys(organizationID)
                .forEach(this.keyDAO::deletePublicKey);

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
    @Override
    public Organization updateOrganization(@NotNull @PathParam("organizationID") UUID organizationID, @Valid @Profiled Organization organization) {
        MethodOutcome outcome = this.client
                .update()
                .resource(organization)
                .withId(organizationID.toString())
                .encodedJson()
                .execute();

        final Organization resource = (Organization) outcome.getResource();
        if (resource == null) {
            throw new WebApplicationException("Unable to update Organization", Response.Status.INTERNAL_SERVER_ERROR);
        }

        return resource;
    }

    private void validateOrganizationBundle(Bundle organizationBundle) {
        // Ensure we have an organization
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
