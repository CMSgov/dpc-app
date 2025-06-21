package gov.cms.dpc.common.hibernate.queue;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import gov.cms.dpc.common.utils.CurrentEngineState;
import io.dropwizard.core.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

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
    @Inject
    DPCQueueManagedSessionFactory getSessionFactory(CurrentEngineState state) {
        return new DPCQueueManagedSessionFactory(this.hibernate.getSessionFactory(), state);
    }

    @Provides
    @Singleton
    CurrentEngineState provideEngineState() {
        return new CurrentEngineState();
    }
}
