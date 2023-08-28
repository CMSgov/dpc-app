package gov.cms.dpc.common.hibernate.consent;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import io.dropwizard.Configuration;

import javax.inject.Singleton;

public class DPCConsentHibernateModule<T extends Configuration & IDPCConsentDatabase> extends DropwizardAwareModule<T> {

    private final DPCConsentHibernateBundle<T> hibernate;

    public DPCConsentHibernateModule(DPCConsentHibernateBundle<T> hibernate) {
        this.hibernate = hibernate;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(DPCConsentHibernateBundle.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    DPCConsentManagedSessionFactory getSessionFactory() {
        return new DPCConsentManagedSessionFactory(this.hibernate.getSessionFactory());
    }

}
