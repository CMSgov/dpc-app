package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.jdbi.ProviderRoleDAO;
import gov.cms.dpc.attribution.resources.AbstractPractitionerRoleResource;
import gov.cms.dpc.common.entities.ProviderRoleEntity;
import io.dropwizard.hibernate.UnitOfWork;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.PractitionerRole;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class PractitionerRoleResource extends AbstractPractitionerRoleResource {

    private final ProviderRoleDAO roleDAO;

    @Inject
    PractitionerRoleResource(ProviderRoleDAO roleDAO) {
        this.roleDAO = roleDAO;
    }

    @Override
    public Bundle getPractitionerRoles(@QueryParam("org") String organizationID, @QueryParam("prov") String providerID) {
        return null;
    }

    @Override
    @UnitOfWork
    @Timed
    @ExceptionMetered
    public PractitionerRole submitPractitionerRole(PractitionerRole role) {
        final ProviderRoleEntity roleEntity = ProviderRoleEntity.fromFHIR(role);
        final ProviderRoleEntity persistedEntity = this.roleDAO.persistRole(roleEntity);

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

    @Override
    public Response deletePractitionerRole(UUID roleID) {
        return null;
    }

    @Override
    @PUT
    @Path("/{roleID}")
    public PractitionerRole updatePractitionerRole(@PathParam("roleID") UUID roleID, PractitionerRole role) {
        return null;
    }
}
