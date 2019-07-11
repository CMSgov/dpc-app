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
import javax.ws.rs.core.Response;
import java.util.UUID;

public class PractitionerRoleResource extends AbstractPractitionerRoleResource {

    private final ProviderRoleDAO roleDAO;

    @Inject
    PractitionerRoleResource(ProviderRoleDAO roleDAO) {
        this.roleDAO = roleDAO;
    }

    @Override
    public Bundle getPractitionerRoles(String organizationID, String providerID) {
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
    public PractitionerRole getPractitionerRole(UUID roleID) {
        return null;
    }

    @Override
    public Response deletePractitionerRole(UUID roleID) {
        return null;
    }

    @Override
    public PractitionerRole updatePractitionerRole(UUID roleID, PractitionerRole role) {
        return null;
    }
}
