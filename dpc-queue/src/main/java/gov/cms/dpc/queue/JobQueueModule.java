package gov.cms.dpc.queue;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import gov.cms.dpc.queue.annotations.AggregatorID;
import gov.cms.dpc.queue.annotations.QueueBatchSize;
import gov.cms.dpc.queue.config.DPCAwsQueueConfiguration;
import gov.cms.dpc.queue.config.DPCQueueConfig;
import gov.cms.dpc.queue.health.JobQueueHealthCheck;
import gov.cms.dpc.queue.service.DataService;
import io.dropwizard.core.Configuration;
import io.github.azagniotov.metrics.reporter.cloudwatch.CloudWatchReporter;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

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
    public void configure() {
        Binder binder = binder();

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
        binder.bind(DataService.class);
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

    @Provides
    DPCAwsQueueConfiguration provideDpcAwsQueueConfiguration() { return configuration().getDpcAwsQueueConfiguration(); }

    @Provides
    CloudWatchAsyncClient provideCloudWatchAsyncClient() {
        return CloudWatchAsyncClient
            .builder()
            .region(Region.of(configuration().getDpcAwsQueueConfiguration().getAwsRegion()))
            .build();
    }

    @Provides
    @Inject
    CloudWatchReporter provideCloudWatchReporter(MetricRegistry metricRegistry, CloudWatchAsyncClient cloudWatchAsyncClient) {
        // If running in local env, use the dry run config.  It will output a log without calling AWS.
        CloudWatchReporter.Builder builder = CloudWatchReporter.forRegistry(
            metricRegistry,
            cloudWatchAsyncClient,
            configuration().getDpcAwsQueueConfiguration().getAwsNamespace()
        )
        .withReportRawCountValue()
        .withZeroValuesSubmission()
        .filter(MetricFilter.contains(configuration().getDpcAwsQueueConfiguration().getQueueSizeMetricName()));

        if( configuration().getDpcAwsQueueConfiguration().getEnvironment().equals("local")) {
            return builder.withDryRun().build();
        } else {
            return builder.build();
        }
    }

    @Provides
    @Inject
    ConsoleReporter provideConsoleReporter(MetricRegistry metricRegistry) {
        return ConsoleReporter.forRegistry(metricRegistry)
            .filter(MetricFilter.contains("JobQueueBatchSize"))
            .build();
    }

    @Provides
    @Inject
    ScheduledReporter provideScheduledReporter(MetricRegistry metricRegistry, CloudWatchAsyncClient cloudWatchAsyncClient) {
        // If AwsMetrics are turned off, use a reporter that writes to the console instead of AWS
        if( configuration().getDpcAwsQueueConfiguration().getEmitAwsMetrics() ) {
            return provideCloudWatchReporter(metricRegistry, cloudWatchAsyncClient);
        } else {
            return provideConsoleReporter(metricRegistry);
        }
    }

    @Provides
    DPCAwsQueueConfiguration provideDPCAwsQueueConfiguration() {
        return configuration().getDpcAwsQueueConfiguration();
    }
}
