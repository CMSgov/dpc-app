package gov.cms.dpc.common.hibernate.attribution;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

public class DPCAbstractDAO<E> extends AbstractDAO<E> {
    public DPCAbstractDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    /**
     * Use this method to get the latest changes of the hibernate database object,
     * bypassing the session cache.
     * Default behavior for hibernate is to cache queries for duration of a session.
     * Meaning, if you call rosterDAO.getEntity() more than once within the same session
     * Hibernate may return the cached value without the latest updates.
     *
     * @param databaseObject instance of database model representing table being queried
     */
    public void refresh(E databaseObject) {
        currentSession().flush();
        currentSession().refresh(databaseObject);
    }

    /**
     * Triggers a transaction synchronization and clears the Hibernate persistence cache.
     * When batching inserts and updates, call this method after reaching your max batch size to prevent storing
     * previous batches in memory any longer than you have to.
     */
    public void cleanUpBatch() {
        currentSession().flush();
        currentSession().clear();
    }
}
