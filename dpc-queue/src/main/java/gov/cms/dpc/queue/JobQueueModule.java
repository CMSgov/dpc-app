package gov.cms.dpc.queue;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
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

        } else if (configuration().getDpcAwsQueueConfiguration() != null) {
            // Has AWS config, running in dpc-aggregation, use AWS distributed queue
            binder.bind(IJobQueue.class)
                .to(AwsDistributedBatchQueue.class)
                .in(Scopes.SINGLETON);

        } else {
            // No AWS config, running in dpc-api, use a distributed queue
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
    @Named("QueueSize")
    @Inject
    ScheduledReporter provideSizeScheduledReporter(MetricRegistry metricRegistry) {
        DPCAwsQueueConfiguration awsConfig = configuration().getDpcAwsQueueConfiguration();
        String metricName = awsConfig.getQueueSizeMetricName();

        // If AwsMetrics are turned off, use a reporter that writes metrics to our logs instead.
        if( awsConfig.getEmitAwsMetrics() ) {
            return provideCloudWatchReporter(metricRegistry, metricName, provideCloudWatchAsyncClient());
        } else {
            return provideSlf4jReporter(metricRegistry, metricName);
        }
    }

    @Provides
    @Named("QueueAge")
    @Inject
    ScheduledReporter provideAgeScheduledReporter(MetricRegistry metricRegistry) {
        return provideSlf4jReporter(metricRegistry, configuration().getDpcAwsQueueConfiguration().getQueueAgeMetricName());
    }

    private Slf4jReporter provideSlf4jReporter(MetricRegistry metricRegistry, String metricName) {
        return Slf4jReporter.forRegistry(metricRegistry)
            .filter(MetricFilter.contains(metricName))
            .withLoggingLevel(Slf4jReporter.LoggingLevel.INFO)
            .build();
    }

    private CloudWatchReporter provideCloudWatchReporter(MetricRegistry metricRegistry, String metricName, CloudWatchAsyncClient cloudWatchAsyncClient) {
        return CloudWatchReporter.forRegistry(
                metricRegistry,
                cloudWatchAsyncClient,
                configuration().getDpcAwsQueueConfiguration().getAwsNamespace()
            )
            .withReportRawCountValue()
            .withZeroValuesSubmission()
            .filter(MetricFilter.contains(metricName))
            .build();
    }

    private CloudWatchAsyncClient provideCloudWatchAsyncClient() {
        return CloudWatchAsyncClient
            .builder()
            .region(Region.of(configuration().getDpcAwsQueueConfiguration().getAwsRegion()))
            .build();
    }
}
