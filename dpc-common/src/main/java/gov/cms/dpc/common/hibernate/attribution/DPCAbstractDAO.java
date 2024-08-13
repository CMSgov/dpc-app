package gov.cms.dpc.common.hibernate.attribution;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

public class DPCAbstractDAO<E> extends AbstractDAO<E> {
    public DPCAbstractDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public void refresh(E databaseObject) {
        currentSession().flush();
        currentSession().refresh(databaseObject);
    }

}
