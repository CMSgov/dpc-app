package gov.cms.dpc.queue;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.queue.annotations.AggregatorID;
import gov.cms.dpc.queue.annotations.QueueBatchSize;
import gov.cms.dpc.queue.health.JobQueueHealthCheck;
import io.dropwizard.Configuration;

import java.util.UUID;

public class JobQueueModule<T extends Configuration & DPCQueueConfig> extends DropwizardAwareModule<T> {

    private final boolean inMemory;
    private final int batchSize;
    private final UUID aggregatorID;

    public JobQueueModule() {
        this.inMemory = false;
        this.batchSize = 100;
        this.aggregatorID = UUID.randomUUID();
    }

    @Override
    public void configure(Binder binder) {
        // Manually bind
        // to the Memory Queue, as a Singleton
        if (this.inMemory) {
            binder.bind(IJobQueue.class)
                    .to(MemoryBatchQueue.class)
                    .in(Scopes.SINGLETON);
        } else {
            binder.bind(IJobQueue.class)
                    .to(DistributedBatchQueue.class)
                    .in(Scopes.SINGLETON);
        }

        // Bind the healthcheck
        binder.bind(JobQueueHealthCheck.class);
    }

    @Provides
    @QueueBatchSize
    int provideBatchSize() {
        return batchSize;
    }

    @Provides
    @AggregatorID
    UUID provideAggregatorID() {
        return aggregatorID;
    }
}
