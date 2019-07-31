package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.RosterEntity;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;

public class RosterDAO extends AbstractDAO<RosterEntity> {

    @Inject
    public RosterDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public RosterEntity persistEntity(RosterEntity roster) {
        return this.persist(roster);
    }
}
