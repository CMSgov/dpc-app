package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleBuilder;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.name.Named;
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
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static gov.cms.dpc.fhir.helpers.FHIRHelpers.getPages;

@Api(value = "Organization", authorizations = @Authorization(value = "access_token"))
@Path("/v1/Organization")
public class OrganizationResource extends AbstractOrganizationResource {

    private final IGenericClient client;
    private final TokenDAO tokenDAO;
    private final PublicKeyDAO keyDAO;

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
        Pair<Organization, List<Endpoint>> validated = validateOrganizationBundle(organizationBundle);
        Organization org = validated.getLeft();

        // Get the org and create it.
        MethodOutcome outcome = this.client.create().resource(org).execute();
        Organization createdOrg = (Organization) outcome.getResource();

        // Add managing org to end points and create
        List<Endpoint> endpoints = validated.getRight();

        BundleBuilder bundleBuilder = new BundleBuilder(client.getFhirContext());
        endpoints.stream()
            .map(ep ->
                ep.setManagingOrganization(new Reference(new IdType("Organization", createdOrg.getIdPart())))
            )
            .forEach(bundleBuilder::addTransactionCreateEntry);
        client.transaction().withBundle(bundleBuilder.getBundle()).execute();

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
        // The org and its endpoints must be deleted together since they reference each other
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("organization", Collections.singletonList(organizationID.toString()));

        Bundle endpointBundle = this.client
            .search()
            .forResource(Endpoint.class)
            .returnBundle(Bundle.class)
            .whereMap(searchParams)
            .encodedJson()
            .execute();
        endpointBundle = getPages(client, endpointBundle);

        // Build a transaction for all of our deletes
        BundleBuilder bundleBuilder = new BundleBuilder(this.client.getFhirContext());
        bundleBuilder.addTransactionDeleteEntry(new IdType("Organization", organizationID.toString()));
        endpointBundle.getEntry().stream()
            .map(entry -> (Endpoint) entry.getResource())
            .forEach(bundleBuilder::addTransactionDeleteEntry);
        this.client.transaction().withBundle(bundleBuilder.getBundle()).execute();

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

    private Pair<Organization, List<Endpoint>> validateOrganizationBundle(Bundle organizationBundle) {
        // Ensure we have an organization
        Organization org = organizationBundle
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType().getPath().equals(DPCResourceType.Organization.getPath()))
                .map(resource -> (Organization) resource)
                .findAny()
                .orElseThrow(() -> new WebApplicationException("Bundle must include Organization", Response.Status.BAD_REQUEST));


        // Make sure we have some endpoints
        final List<Endpoint> endpoints = organizationBundle
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType().getPath().equals(DPCResourceType.Endpoint.getPath()))
                .map(resource -> (Endpoint) resource)
                .collect(Collectors.toList());

        if (endpoints.isEmpty()) {
            throw new WebApplicationException("Organization must have at least 1 endpoint", Response.Status.BAD_REQUEST);
        }

        return Pair.of(org, endpoints);
    }
}
