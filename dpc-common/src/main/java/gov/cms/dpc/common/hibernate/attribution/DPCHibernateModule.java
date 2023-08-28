package gov.cms.dpc.common.hibernate.attribution;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;

import javax.inject.Singleton;


public class DPCHibernateModule<T extends Configuration & IDPCDatabase> extends DropwizardAwareModule<T> {

    private final DPCHibernateBundle<T> hibernate;

    public DPCHibernateModule(DPCHibernateBundle<T> hibernate) {
        this.hibernate = hibernate;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(DPCHibernateBundle.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    DPCManagedSessionFactory getSessionFactory() {
        return new DPCManagedSessionFactory(this.hibernate.getSessionFactory());
    }

    @Provides
    @Singleton
    ManagedDataSource provideDataSource(DataSourceFactory factory) {
        return factory.build(getEnvironment().metrics(), "tested-things");
    }

    @Provides
    @Singleton
    DataSourceFactory provideFactory() {
        return getConfiguration().getDatabase();
    }
}
