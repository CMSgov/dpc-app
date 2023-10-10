package gov.cms.dpc.common.hibernate.auth;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import io.dropwizard.Configuration;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

import javax.inject.Singleton;

public class DPCAuthHibernateModule<T extends Configuration & IDPCAuthDatabase> extends DropwizardAwareModule<T> {

    private final DPCAuthHibernateBundle<T> hibernate;

    public DPCAuthHibernateModule(DPCAuthHibernateBundle<T> hibernate) {
        this.hibernate = hibernate;
    }

    @Override
    public void configure() {
        binder().bind(DPCAuthHibernateBundle.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    DPCAuthManagedSessionFactory getSessionFactory() {
        return new DPCAuthManagedSessionFactory(this.hibernate.getSessionFactory());
    }

}
