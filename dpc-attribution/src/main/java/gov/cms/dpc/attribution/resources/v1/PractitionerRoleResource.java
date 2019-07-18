package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.jdbi.ProviderRoleDAO;
import gov.cms.dpc.attribution.resources.AbstractPractitionerRoleResource;
import gov.cms.dpc.common.entities.ProviderRoleEntity;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.PractitionerRole;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

public class PractitionerRoleResource extends AbstractPractitionerRoleResource {

    private final ProviderRoleDAO roleDAO;

    @Inject
    PractitionerRoleResource(ProviderRoleDAO roleDAO) {
        this.roleDAO = roleDAO;
    }

    @GET
    @UnitOfWork
    @FHIR
    @Timed
    @ExceptionMetered
    @Override
    @ApiOperation(value = "Fetch PractitionerRole resources", notes = "Fetches PractitionerRole resources for either a given Practitioner or Organization." +
            "<p>Either a Provider or Organization NPI must be given. If both are given, it's the equivalent to asserting that a relationships exists between the two entities")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Either a Provider NPI or Organization must be given")
    })
    public Bundle getPractitionerRoles(@ApiParam(value = "Organization NPI")
                                       @QueryParam("organization") String organizationReference,
                                       @ApiParam(value = "Provider NPI")
                                       @QueryParam("practitioner") String providerReference) {

        UUID organizationID = null;
        UUID providerID = null;
        if (organizationReference == null && providerReference == null) {
            throw new WebApplicationException("Must have either Organization ID or Practitioner ID for searching", Response.Status.BAD_REQUEST);
        }

        if (organizationReference != null) {
            organizationID = FHIRExtractors.getEntityUUID(organizationReference);
        }

        if (providerReference != null) {
            providerID = FHIRExtractors.getEntityUUID(providerReference);
        }

        final List<ProviderRoleEntity> roles = this.roleDAO.findRoles(organizationID, providerID);

        final Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(roles.size());

        roles.forEach(role -> bundle.addEntry().setResource(role.toFHIR()));

        return bundle;
    }

    @Override
    @UnitOfWork
    @POST
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Create a PractitionerRole resource.", notes = "Creates an association between the given Provider and Organization")
    @ApiResponses(@ApiResponse(code = 201, message = "Resource was created"))
    public Response submitPractitionerRole(PractitionerRole role) {
        final ProviderRoleEntity roleEntity = ProviderRoleEntity.fromFHIR(role);
        final ProviderRoleEntity persistedEntity;
        persistedEntity = this.roleDAO.persistRole(roleEntity);

        return Response.status(Response.Status.CREATED).entity(persistedEntity.toFHIR()).build();
    }

    @Override
    @GET
    @Path("/{roleID}")
    @FHIR
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch PractitionerRole resource", notes = "Fetches a specific PractitionerRole resource.")
    @ApiResponses(@ApiResponse(code = 404, message = "Could not find resource"))
    public PractitionerRole getPractitionerRole(@ApiParam(value = "Practitioner Role resource ID", required = true) @PathParam("roleID") UUID roleID) {
        final ProviderRoleEntity entity = this.roleDAO.fetchRole(roleID);
        if (entity == null) {
            throw new WebApplicationException(String.format("Cannot find role with ID: %s", roleID), Response.Status.NOT_FOUND);
        }

        return entity.toFHIR();
    }

    @DELETE
    @Path("/{roleID}")
    @FHIR
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @Override
    @ApiOperation(value = "Remove PractitionerRole resource", notes = "Removes specific resource, which is equivalent to removing the relationship between the provider and organization")
    public Response deletePractitionerRole(@ApiParam(value = "Practitioner Role resource ID", required = true) @PathParam("roleID") UUID roleID) {
        final boolean removed = this.roleDAO.removeRole(roleID);
        if (removed) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        return Response.status(Response.Status.NOT_FOUND).entity(String.format("Could not find Practitioner role: %s", roleID)).build();
    }
}
