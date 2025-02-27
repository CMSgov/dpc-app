package gov.cms.dpc.common.hibernate.attribution;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

import javax.inject.Singleton;


public class DPCHibernateModule<T extends Configuration & IDPCDatabase> extends DropwizardAwareModule<T> {

    private final DPCHibernateBundle<T> hibernate;

    public DPCHibernateModule(DPCHibernateBundle<T> hibernate) {
        this.hibernate = hibernate;
    }

    @Override
    public void configure() {
        binder().bind(DPCHibernateBundle.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    DPCManagedSessionFactory getSessionFactory() {
        return new DPCManagedSessionFactory(this.hibernate.getSessionFactory());
    }

    @Provides
    @Singleton
    ManagedDataSource provideDataSource(DataSourceFactory factory) {
        return factory.build(environment().metrics(), "tested-things");
    }

    @Provides
    @Singleton
    DataSourceFactory provideFactory() {
        return configuration().getDatabase();
    }
}
