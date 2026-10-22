package gov.cms.dpc.common.hibernate.attribution;

import com.google.inject.Provides;
import com.google.inject.name.Named;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import jakarta.inject.Singleton;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;


public class DPCHibernateModule<T extends Configuration & IDPCDatabase> extends DropwizardAwareModule<T> {

    private final DPCStu3HibernateBundle<T> stu3Hibernate;
    private final DPCR4HibernateBundle<T> r4Hibernate;

    public DPCHibernateModule(DPCStu3HibernateBundle<T> stu3Hibernate, DPCR4HibernateBundle<T> r4Hibernate) {
        this.stu3Hibernate = stu3Hibernate;
        this.r4Hibernate = r4Hibernate;
    }

    @Override
    public void configure() {
        binder().bind(DPCStu3HibernateBundle.class).toInstance(stu3Hibernate);
        binder().bind(DPCR4HibernateBundle.class).toInstance(r4Hibernate);
    }

    @Provides
    @Singleton
    @Named("stu3")
    DPCManagedSessionFactory getStu3SessionFactory() {
        return new DPCManagedSessionFactory(this.stu3Hibernate.getSessionFactory());
    }

    @Provides
    @Singleton
    @Named("r4")
    DPCManagedSessionFactory getR4SessionFactory() {
        return new DPCManagedSessionFactory(this.r4Hibernate.getSessionFactory());
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
