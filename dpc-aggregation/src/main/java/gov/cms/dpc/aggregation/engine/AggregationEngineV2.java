package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.queue.JobQueue;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.UUID;

/**
 * The top level of the Aggregation Engine. {@link ResourceFetcher} does the fetching from
 * BlueButton and {@link ResourceWriter} does the writing.
 *
 * Implementation Notes:
 * - There is a single flow that does the work for a job
 * - It starts with an iteration of resource types in a job and produces a series of JobQueueBatchFile for that resource type
 */
public class AggregationEngineV2 implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AggregationEngineV2.class);

    private final UUID aggregatorID = UUID.randomUUID();

    private final JobQueue queue;
    private final BlueButtonClient bbclient;
    private final OperationsConfig operationsConfig;
    private final FhirContext fhirContext;
    private final Meter resourceMeter;
    private final Meter operationalOutcomeMeter;
    private Disposable subscribe;

    /**
     * Create an engine.
     *
     * @param bbclient    - {@link BlueButtonClient } to use
     * @param queue       - {@link JobQueue} that will direct the work done
     * @param fhirContext - {@link FhirContext} for DSTU3 resources
     * @param metricRegistry - {@link MetricRegistry} for metrics
     * @param operationsConfig  - The {@link OperationsConfig} to use for writing the output files
     */
    @Inject
    public AggregationEngineV2(BlueButtonClient bbclient, JobQueue queue, FhirContext fhirContext, MetricRegistry metricRegistry, OperationsConfig operationsConfig) {
        this.queue = queue;
        this.bbclient = bbclient;
        this.fhirContext = fhirContext;
        this.operationsConfig = operationsConfig;

        // Metrics
        final var metricFactory = new MetricMaker(metricRegistry, AggregationEngine.class);
        resourceMeter = metricFactory.registerMeter("resourceFetched");
        operationalOutcomeMeter = metricFactory.registerMeter("operationalOutcomes");
    }

    /**
     * Run the engine. Part of the Runnable interface.
     */
    @Override
    public void run() {
        // Run loop
        logger.info("Starting aggregation engine with exportPath:\"{}\" resourcesPerFile:{} ",
                operationsConfig.getExportPath(),
                operationsConfig.getResourcesPerFileCount());
        setGlobalErrorHandler();
        this.pollQueue();
    }

    /**
     * Stop the engine.
     */
    public void stop() {
        logger.info("Shutting down aggregation engine");
        this.subscribe.dispose();
    }

    /**
     * Setup a global handler to catch the UndeliverableException case. Can be called from anywhere.
     */
    static void setGlobalErrorHandler() {
        RxJavaPlugins.setErrorHandler(AggregationEngineV2::errorHandler);
    }

    /**
     * The main run-loop of the engine.
     */
    private void pollQueue() {

    }

    /**
     * Global error handler. Needed to catch undeliverable exceptions.
     *
     * See: https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling
     *
     * @param e is the exception thrown
     */
    private static void errorHandler(Throwable e) {
        // Undeliverable Exceptions may happen because of parallel execution. One thread will
        // throw an exception which will cause the job to fail and (close its consumer).
        // Another thread will throw an exception as well which will be undeliverable
        if (e instanceof UndeliverableException) {
            e = e.getCause();
        }
        if (e instanceof IOException) {
            // Expected: network problem or API that throws on cancellation
            return;
        }
        if (e instanceof InterruptedException) {
            // Expected: some blocking code was interrupted by a dispose call
            return;
        }
        if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
            // that's likely a bug in the application
            Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            return;
        }
        if (e instanceof IllegalStateException) {
            // that's a bug in RxJava or in a custom operator
            Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            return;
        }
        logger.warn("Undeliverable exception received: ", e);
    }
}
