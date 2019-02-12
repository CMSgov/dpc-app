package gov.cms.dpc.queue;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobQueueModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(JobQueueModule.class);

    private final boolean inMemory;

    public JobQueueModule() {
        this.inMemory = true;
    }

    public JobQueueModule(boolean inMemory) {
        this.inMemory = inMemory;
    }

    @Override
    protected void configure() {

        // bind it up
//        logger.debug("Binding {} to Queue", clazz.getName());
//        Multibinder.newSetBinder(binder(), JobQueue.class)
//                .addBinding()
//                .to(new TypeLiteral<JobQueue<T>>() {
//                })
//        .in(Scopes.SINGLETON);

        // Manually bind to the Memory Queue, as a Singleton
        bind(JobQueue.class)
                .to(MemoryQueue.class)
                .in(Scopes.SINGLETON);
    }
}
