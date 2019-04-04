package gov.cms.dpc.common.hibernate;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import io.dropwizard.Configuration;
import org.hibernate.SessionFactory;

import javax.inject.Singleton;


public class DPCHibernateModule<T extends Configuration & IDPCDatabase> extends DropwizardAwareModule<T> {

    public DPCHibernateModule() {
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(DPCHibernateBundle.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    SessionFactory getSessionFactory(DPCHibernateBundle hibernate) {
        // This is necessary because the session factory doesn't load on its own.
        // I'm really not sure how to fix this, I think it's due to the interaction with the Proxy Factory
        try {
            //noinspection unchecked
            hibernate.run(getConfiguration(), getEnvironment());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return hibernate.getSessionFactory();
    }
}
