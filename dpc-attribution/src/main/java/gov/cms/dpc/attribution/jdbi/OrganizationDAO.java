package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;

public class OrganizationDAO extends AbstractDAO<OrganizationEntity> {

    @Inject
    OrganizationDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public void registerOrganization(Organization resource) {
        final OrganizationEntity entity = new OrganizationEntity().fromFHIR(resource);
        persist(entity);
    }
}
