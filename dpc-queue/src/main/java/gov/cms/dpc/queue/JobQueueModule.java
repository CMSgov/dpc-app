package gov.cms.dpc.queue;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import io.dropwizard.Configuration;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobQueueModule<T extends Configuration & DPCQueueConfig> extends DropwizardAwareModule<T> {

    private static final Logger logger = LoggerFactory.getLogger(JobQueueModule.class);

    private final boolean inMemory;
    private Config config;

    public JobQueueModule() {
        this.inMemory = false;
//
////        config.setTransportMode(TransportMode.EPOLL);
//        config.useSingleServer().setAddress("redis://localhost:6379");
    }

    @Override
    public void configure(Binder binder) {
        this.config = getConfiguration().getQueueConfig();

        // Manually bind
        // to the Memory Queue, as a Singleton
        if (this.inMemory) {
            binder.bind(JobQueue.class)
                    .to(MemoryQueue.class)
                    .in(Scopes.SINGLETON);
        } else {
            binder.bind(JobQueue.class)
                    .to(DistributedQueue.class)
                    .in(Scopes.SINGLETON);
        }
    }

    @Provides
    RedissonClient provideClient() {
        return Redisson.create(config);
    }
}
