package gov.cms.dpc.common.hibernate.queue;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import io.dropwizard.core.Configuration;

public class DPCQueueHibernateModule<T extends Configuration & IDPCQueueDatabase> extends DropwizardAwareModule<T> {

    private final DPCQueueHibernateBundle<T> hibernate;

    public DPCQueueHibernateModule(DPCQueueHibernateBundle<T> hibernate) {
        this.hibernate = hibernate;
    }

    @Override
    public void configure() {
        binder().bind(DPCQueueHibernateBundle.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    DPCQueueManagedSessionFactory getSessionFactory() {
        return new DPCQueueManagedSessionFactory(this.hibernate.getSessionFactory());
    }

}
