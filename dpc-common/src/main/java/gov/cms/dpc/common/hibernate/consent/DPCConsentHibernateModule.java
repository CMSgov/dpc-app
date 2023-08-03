package gov.cms.dpc.common.hibernate.consent;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import io.dropwizard.Configuration;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

import javax.inject.Singleton;

public class DPCConsentHibernateModule<T extends Configuration & IDPCConsentDatabase> extends DropwizardAwareModule<T> {

    private final DPCConsentHibernateBundle<T> hibernate;

    public DPCConsentHibernateModule(DPCConsentHibernateBundle<T> hibernate) {
        this.hibernate = hibernate;
    }

    @Override
    protected void configure() {
        binder().bind(DPCConsentHibernateBundle.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    DPCConsentManagedSessionFactory getSessionFactory() {
        return new DPCConsentManagedSessionFactory(this.hibernate.getSessionFactory());
    }

}
