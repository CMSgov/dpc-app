package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.RosterEntity;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;

public class RosterDAO extends AbstractDAO<RosterEntity> {

    @Inject
    public RosterDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public RosterEntity persistEntity(RosterEntity roster) {
        return this.persist(roster);
    }

    public Optional<RosterEntity> getEntity(UUID rosterID) {
        return Optional.ofNullable(this.get(rosterID));
    }
}
