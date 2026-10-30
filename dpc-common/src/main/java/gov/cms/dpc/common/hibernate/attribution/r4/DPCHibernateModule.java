package gov.cms.dpc.common.hibernate.attribution.r4;

import com.google.inject.Provides;
import com.google.inject.name.Named;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.common.hibernate.attribution.DPCR4HibernateBundle;
import gov.cms.dpc.common.hibernate.attribution.IDPCDatabase;
import io.dropwizard.core.Configuration;
import jakarta.inject.Singleton;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

public class DPCHibernateModule<T extends Configuration & IDPCDatabase> extends DropwizardAwareModule<T> {

    private final DPCR4HibernateBundle<T> r4Hibernate;

    public DPCHibernateModule(DPCR4HibernateBundle<T> r4Hibernate) {
        this.r4Hibernate = r4Hibernate;
    }

    @Override
    public void configure() {
        binder().bind(DPCR4HibernateBundle.class).toInstance(r4Hibernate);
    }

    @Provides
    @Singleton
    @Named("r4")
    DPCManagedSessionFactory getR4SessionFactory() {
        return new DPCManagedSessionFactory(this.r4Hibernate.getSessionFactory());
    }
}
