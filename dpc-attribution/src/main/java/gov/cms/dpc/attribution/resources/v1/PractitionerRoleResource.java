package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.jdbi.ProviderRoleDAO;
import gov.cms.dpc.attribution.resources.AbstractPractitionerRoleResource;
import gov.cms.dpc.common.entities.ProviderRoleEntity;
import gov.cms.dpc.fhir.FHIRExtractors;
import io.dropwizard.hibernate.UnitOfWork;
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
    @Timed
    @ExceptionMetered
    @Override
    public Bundle getPractitionerRoles(@QueryParam("organization") String organizationReference, @QueryParam("practitioner") String providerReference) {

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
    @Timed
    @ExceptionMetered
    public PractitionerRole submitPractitionerRole(PractitionerRole role) {
        final ProviderRoleEntity roleEntity = ProviderRoleEntity.fromFHIR(role);
        final ProviderRoleEntity persistedEntity;
        persistedEntity = this.roleDAO.persistRole(roleEntity);

        return persistedEntity.toFHIR();
    }

    @Override
    @GET
    @Path("/{roleID}")
    @UnitOfWork
    @Timed
    @ExceptionMetered
    public PractitionerRole getPractitionerRole(@PathParam("roleID") UUID roleID) {
        final ProviderRoleEntity entity = this.roleDAO.fetchRole(roleID);
        if (entity == null) {
            throw new WebApplicationException(String.format("Cannot find role with ID: %s", roleID), Response.Status.NOT_FOUND);
        }

        return entity.toFHIR();
    }

    @DELETE
    @Path("/{roleID}")
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @Override
    public Response deletePractitionerRole(@PathParam("roleID") UUID roleID) {
        final boolean removed = this.roleDAO.removeRole(roleID);
        if (removed) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        return Response.status(Response.Status.NOT_FOUND).entity(String.format("Could not find Practitioner role: %s", roleID)).build();
    }
}
