package gov.cms.dpc.queue;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.queue.annotations.HealthCheckQuery;
import gov.cms.dpc.queue.annotations.QueueBatchSize;
import gov.cms.dpc.queue.health.JobQueueHealthCheck;
import io.dropwizard.Configuration;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class JobQueueModule<T extends Configuration & DPCQueueConfig> extends DropwizardAwareModule<T> {

    private final boolean inMemory;
    private final int batchSize;
    private Config config;

    public JobQueueModule() {
        this.inMemory = false;
        this.batchSize = 100;
    }

    @Override
    public void configure(Binder binder) {
        this.config = getConfiguration().getQueueConfig();

        // Manually bind
        // to the Memory Queue, as a Singleton
        if (this.inMemory) {
            binder.bind(JobQueueInterface.class)
                    .to(MemoryBatchQueue.class)
                    .in(Scopes.SINGLETON);
        } else {
            binder.bind(JobQueueInterface.class)
                    .to(DatabaseQueue.class)
                    .in(Scopes.SINGLETON);
        }

        // Bind the healthcheck
        binder.bind(JobQueueHealthCheck.class);
    }

    @Provides
    RedissonClient provideClient() {
        return Redisson.create(config);
    }

    @Provides
    @HealthCheckQuery
    String provideHealthQuery() {
        // TODO: Eventually, this should get pulled out into the config file
        return "SELECT 1 from job_queue;";
    }

    @Provides
    @QueueBatchSize
    int provideBatchSize() {
        return batchSize;
    }
}
