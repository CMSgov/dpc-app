package gov.cms.dpc.macaroons.store.hiberate;

import gov.cms.dpc.macaroons.store.AbstractStoreTest;
import gov.cms.dpc.macaroons.store.hibernate.HibernateKeyStore;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.security.SecureRandom;

class HibernateStoreTest extends AbstractStoreTest {

    HibernateStoreTest() {
        super(new HibernateKeyStore(buildSessionFactory(), new SecureRandom()));
    }

    static SessionFactory buildSessionFactory() {
        final Configuration conf = new Configuration();
        return conf.configure().buildSessionFactory();
    }
}
