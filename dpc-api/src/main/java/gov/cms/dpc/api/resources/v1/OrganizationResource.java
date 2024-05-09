package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
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
import org.hl7.fhir.dstu3.model.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @AdminOperation
    @Override
    public Organization submitOrganization(@FHIRParameter(name = "resource") @NotNull Bundle organizationBundle) {
        // Validate bundle
        validateOrganizationBundle(organizationBundle);

        final Parameters parameters = new Parameters();
        parameters.addParameter().setName("resource").setResource(organizationBundle);
        return this.client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(parameters)
                .returnResourceType(Organization.class)
                .encodedJson()
                .execute();
    }

    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    public Bundle orgSearch(@Auth OrganizationPrincipal organization) {
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


        // Make sure we have some endpoints
        final List<Resource> endpoints = organizationBundle
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType().getPath().equals(DPCResourceType.Endpoint.getPath()))
                .collect(Collectors.toList());

        if (endpoints.isEmpty()) {
            throw new WebApplicationException("Organization must have at least 1 endpoint", Response.Status.BAD_REQUEST);
        }
    }
}
