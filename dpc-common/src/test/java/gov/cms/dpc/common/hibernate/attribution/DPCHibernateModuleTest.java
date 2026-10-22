package gov.cms.dpc.common.hibernate.attribution;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DPCHibernateModuleTest {

    @Test
    @SuppressWarnings("unchecked")
    void injectsSeparateFactoriesFromRegisteredBundles() {
        // Make sure Guice can load the R4 and Stu3 Hibernate bundles at the same time.  I don't want to deal with
        // debugging weird injection errors if there ends up being an issue with this.
        // We can get rid of this once Stu3 is deprecated.

        DPCStu3HibernateBundle<TestConfiguration> stu3Bundle = mock(DPCStu3HibernateBundle.class);
        DPCR4HibernateBundle<TestConfiguration> r4Bundle = mock(DPCR4HibernateBundle.class);
        SessionFactory stu3Factory = mock(SessionFactory.class);
        SessionFactory r4Factory = mock(SessionFactory.class);
        when(stu3Bundle.getSessionFactory()).thenReturn(stu3Factory);
        when(r4Bundle.getSessionFactory()).thenReturn(r4Factory);

        Injector injector = Guice.createInjector(new DPCHibernateModule<>(stu3Bundle, r4Bundle));
        Key<DPCManagedSessionFactory> stu3Key = Key.get(DPCManagedSessionFactory.class, Names.named("stu3"));
        Key<DPCManagedSessionFactory> r4Key = Key.get(DPCManagedSessionFactory.class, Names.named("r4"));
        DPCManagedSessionFactory stu3ManagedFactory = injector.getInstance(stu3Key);
        DPCManagedSessionFactory r4ManagedFactory = injector.getInstance(r4Key);

        assertSame(stu3Factory, stu3ManagedFactory.getSessionFactory());
        assertSame(r4Factory, r4ManagedFactory.getSessionFactory());
        assertSame(stu3ManagedFactory, injector.getInstance(stu3Key));
        assertSame(r4ManagedFactory, injector.getInstance(r4Key));
        assertSame(stu3Bundle, injector.getInstance(DPCStu3HibernateBundle.class));
        assertSame(r4Bundle, injector.getInstance(DPCR4HibernateBundle.class));
    }

    private static class TestConfiguration extends Configuration implements IDPCDatabase {
        @Override
        public DataSourceFactory getDatabase() {
            return new DataSourceFactory();
        }
    }
}
