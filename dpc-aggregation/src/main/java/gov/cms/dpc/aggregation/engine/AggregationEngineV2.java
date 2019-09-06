package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.queue.JobQueueInterface;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The top level of the Aggregation Engine. {@link ResourceFetcher} does the fetching from
 * BlueButton and {@link ResourceWriter} does the writing.
 *
 * Implementation Notes:
 * - There is a single flow that does the work for a job
 * - It starts with an iteration of resource types in a job and produces a series of JobQueueBatchFile for that resource type
 * - Partial job batches are saved out and written along the way
 * - When the aggregator shuts down, a batch is paused and another aggregator can claim the batch to continue processing
 */
public class AggregationEngineV2 implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AggregationEngineV2.class);

    private final UUID aggregatorID = UUID.randomUUID();

    private final JobQueueInterface queue;
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
     * @param queue       - {@link JobQueueInterface} that will direct the work done
     * @param fhirContext - {@link FhirContext} for DSTU3 resources
     * @param metricRegistry - {@link MetricRegistry} for metrics
     * @param operationsConfig  - The {@link OperationsConfig} to use for writing the output files
     */
    @Inject
    public AggregationEngineV2(BlueButtonClient bbclient, JobQueueInterface queue, FhirContext fhirContext, MetricRegistry metricRegistry, OperationsConfig operationsConfig) {
        this.queue = queue;
        this.bbclient = bbclient;
        this.fhirContext = fhirContext;
        this.operationsConfig = operationsConfig;

        // Metrics
        final var metricFactory = new MetricMaker(metricRegistry, AggregationEngineV2.class);
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
     * The main run-loop of the engine.
     */
    private void pollQueue() {
        subscribe = Observable.fromCallable(() -> this.queue.workBatch(aggregatorID))
                .doOnNext(job -> logger.trace("Polling queue for job"))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .repeatWhen(completed -> {
                    logger.debug("No job, polling again in 2 seconds");
                    return completed.delay(2, TimeUnit.SECONDS);
                })
                .subscribe(this::processJobBatch, error -> logger.error("Unable to complete job.", error));
    }

    /**
     * Loops over the partials of a job batch and handles completed, error, and paused job scenarios
     * @param job - the job to process
     */
    private void processJobBatch(JobQueueBatch job) {
        try {
            logger.info("Processing job {} batch {}, exporting to: {}.", job.getJobID(), job.getBatchID(), this.operationsConfig.getExportPath());
            logger.debug("Has {} attributed beneficiaries", job.getPatients().size());

            final var errorCounter = new AtomicInteger();
            Optional<String> nextPatientID = job.fetchNextBatch(aggregatorID);

            // Stop processing when no patients or early shutdown
            while ( nextPatientID.isPresent() && !this.subscribe.isDisposed() ) {
                this.processJobBatchPartial(job, nextPatientID.get(), errorCounter);
                nextPatientID = job.fetchNextBatch(aggregatorID);
            }

            // Finish processing the batch
            if ( nextPatientID.isEmpty() ) {
                logger.info("COMPLETED job {} batch {}", job.getJobID(), job.getBatchID());
                this.queue.completeBatch(job, aggregatorID);
            } else if ( this.subscribe.isDisposed() ) {
                logger.info("PAUSED job {} batch {}", job.getJobID(), job.getBatchID());
                this.queue.pauseBatch(job, aggregatorID);
            }
        } catch (Exception error) {
            logger.error("FAILED job {} batch {}", job.getJobID(), job.getBatchID(), error);
            this.queue.failBatch(job, aggregatorID);
        }
    }

    /**
     * Processes a partial of a job batch. Marks the partial as completed upon processing
     * @param job - the job to process
     * @param patientID - The current patient id processing
     * @param errorCounter - The current error count
     */
    private void processJobBatchPartial(JobQueueBatch job, String patientID, AtomicInteger errorCounter) {
        final var results = Flowable.fromIterable(job.getResourceTypes())
                .flatMap(resourceType -> completeResource(job, patientID, resourceType, errorCounter))
                .toList()
                .blockingGet(); // Wait on the main thread until completion
        this.queue.completePartialBatch(job, aggregatorID);
    }

    /**
     * Fetch and write a specific resource type
     * @param job context
     * @param resourceType to process
     * @param errorCounter to count the OperationalOutcome JobQueueBatchFile
     * @return A new job result observable
     */
    private Flowable<JobQueueBatchFile> completeResource(JobQueueBatch job, String patientID, ResourceType resourceType, AtomicInteger errorCounter) {
        // Make this flow hot (ie. only called once) when multiple subscribers attach
        final var fetcher = new ResourceFetcher(bbclient, job.getJobID(), job.getBatchID(), resourceType, operationsConfig);
        final Flowable<Resource> mixedFlow = fetcher.fetchResources(patientID);
        final var connectableMixedFlow = mixedFlow.publish().autoConnect(2);

        // Batch the non-error resources into files
        final var counter = new AtomicInteger();
        final var writer = new ResourceWriter(fhirContext, job, resourceType, operationsConfig);
        final Flowable<JobQueueBatchFile> resourceFlow = connectableMixedFlow.compose((upstream) -> bufferAndWrite(upstream, writer, counter, resourceMeter));

        // Batch the error resources into files
        final var errorWriter = new ResourceWriter(fhirContext, job, ResourceType.OperationOutcome, operationsConfig);
        final Flowable<JobQueueBatchFile> outcomeFlow = connectableMixedFlow.compose((upstream) -> bufferAndWrite(upstream, errorWriter, errorCounter, operationalOutcomeMeter));

        // Merge the resultant flows
        return resourceFlow.mergeWith(outcomeFlow);
    }

    /**
     * This part of the flow chain buffers resources and writes them in batches to a file
     *
     * @param writer - the writer to use
     * @param counter - the sequence counter
     * @param meter - a meter on the number of resources
     * @return a transformed flow
     */
    private Publisher<JobQueueBatchFile> bufferAndWrite(Flowable<Resource> upstream, ResourceWriter writer, AtomicInteger counter, Meter meter) {
        return upstream
                .filter(r -> r.getResourceType() == writer.getResourceType())
                .buffer(operationsConfig.getResourcesPerFileCount())
                .doOnNext(outcomes -> meter.mark(outcomes.size()))
                .map(batch -> writer.writeBatch(counter, batch));
    }

    /**
     * Setup a global handler to catch the UndeliverableException case. Can be called from anywhere.
     */
    private static void setGlobalErrorHandler() {
        RxJavaPlugins.setErrorHandler(AggregationEngineV2::errorHandler);
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
