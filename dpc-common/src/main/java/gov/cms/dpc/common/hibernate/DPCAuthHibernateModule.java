package gov.cms.dpc.common.hibernate;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import io.dropwizard.Configuration;

import javax.inject.Singleton;

public class DPCAuthHibernateModule<T extends Configuration & IDPCAuthDatabase> extends DropwizardAwareModule<T> {

    private final DPCAuthHibernateBundle<T> hibernate;

    public DPCAuthHibernateModule(DPCAuthHibernateBundle<T> hibernate) {
        this.hibernate = hibernate;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(DPCAuthHibernateBundle.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    DPCAuthManagedSessionFactory getSessionFactory() {
        return new DPCAuthManagedSessionFactory(this.hibernate.getSessionFactory());
    }

}
