package gov.cms.dpc.common.hibernate.attribution.stu3;

import com.google.inject.Provides;
import com.google.inject.name.Named;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.common.hibernate.attribution.DPCStu3HibernateBundle;
import gov.cms.dpc.common.hibernate.attribution.IDPCDatabase;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import jakarta.inject.Singleton;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;


public class DPCHibernateModule<T extends Configuration & IDPCDatabase> extends DropwizardAwareModule<T> {

    private final DPCStu3HibernateBundle<T> stu3Hibernate;

    public DPCHibernateModule(DPCStu3HibernateBundle<T> stu3Hibernate) {
        this.stu3Hibernate = stu3Hibernate;
    }

    @Override
    public void configure() {
        binder().bind(DPCStu3HibernateBundle.class).toInstance(stu3Hibernate);
    }

    @Provides
    @Singleton
    @Named("stu3")
    DPCManagedSessionFactory getStu3SessionFactory() {
        return new DPCManagedSessionFactory(this.stu3Hibernate.getSessionFactory());
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
