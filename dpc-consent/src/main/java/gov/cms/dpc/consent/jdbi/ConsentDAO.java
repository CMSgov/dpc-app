package gov.cms.dpc.consent.jdbi;

import gov.cms.dpc.common.entities.ConsentEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;

public class ConsentDAO extends AbstractDAO<ConsentEntity> {

    @Inject
    ConsentDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }
}
