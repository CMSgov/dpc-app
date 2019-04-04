package gov.cms.dpc.queue;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobQueueModule extends PrivateModule {

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

        // Expose things
        expose(JobQueue.class);
    }

    @Provides
    RedissonClient provideClient() {
        final Config config = new Config();

        config.setTransportMode(TransportMode.EPOLL);
        config.useSingleServer().setAddress("redis://localhost:6379");

        return Redisson.create(config);
    }
}
