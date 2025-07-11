package gov.cms.dpc.aggregation.engine;

import com.newrelic.api.agent.Trace;
import gov.cms.dpc.aggregation.util.AggregationUtils;
import gov.cms.dpc.common.MDCConstants;
import gov.cms.dpc.common.logging.SplunkTimestamp;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.annotations.AggregatorID;
import gov.cms.dpc.queue.models.JobQueueBatch;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The top level of the Aggregation Engine. {@link ResourceFetcher} does the fetching from
 * BlueButton and {@link ResourceWriter} does the writing.
 * <p>
 * Implementation Notes:
 * - There is a single flow that does the work for a job
 * - It starts with an iteration of resource types in a job and produces a series of JobQueueBatchFile for that resource type
 * - Partial job batches are saved out and written along the way
 * - When the aggregator shuts down, a batch is paused and another aggregator can claim the batch to continue processing
 */
public class AggregationEngine implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);

    private final UUID aggregatorID;
    private final IJobQueue queue;
    private final OperationsConfig operationsConfig;
    private final JobBatchProcessor jobBatchProcessor;
    private Disposable subscribe;
    private final AtomicReference<Optional<JobQueueBatch>> currentBatch = new AtomicReference<>(Optional.empty());

    /**
     * The initial value is set to true so when the aggregation instance starts up,
     * it's not in an unhealthy state (determined by the AggregationEngineHealthCheck)
     */
    protected AtomicBoolean queueRunning = new AtomicBoolean(true);

    /**
     * Create an engine.
     *
     * @param aggregatorID      - The ID of the current working aggregator
     * @param queue             - {@link IJobQueue} that will direct the work done
     * @param operationsConfig  - The {@link OperationsConfig} to use for writing the output files
     * @param jobBatchProcessor - {@link JobBatchProcessor} contains all the job processing logic
     */
    @Inject
    public AggregationEngine(@AggregatorID UUID aggregatorID, IJobQueue queue, OperationsConfig operationsConfig, JobBatchProcessor jobBatchProcessor) {
        this.aggregatorID = aggregatorID;
        this.queue = queue;
        this.operationsConfig = operationsConfig;
        this.jobBatchProcessor = jobBatchProcessor;
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
        queueRunning.set(true);
        this.pollQueue();
    }

    /**
     * Stop the engine.  This is usually called from the AggregationManager in a Jetty shutdown thread.  This means it
     * can be called at the same time the main thread is either polling or processing a patient.
     */
    public void stop() {
        logger.info("Shutting down aggregation engine from thread: {}", Thread.currentThread().getId());

        // Stop and dispose of queue
        queueRunning.set(false);
        if (this.subscribe != null) {
            this.subscribe.dispose();
        }

        // If a batch is currently running, mark it paused.
        Optional<JobQueueBatch> optionalBatch = this.currentBatch.get();
        optionalBatch.ifPresent(jobQueueBatch -> {
            logger.info("Pausing batch: {}", jobQueueBatch.getBatchID());
            this.queue.pauseBatch(jobQueueBatch, aggregatorID);
        });
    }

    public boolean isRunning() {
        return queueRunning.get();
    }

    /**
     * The main run-loop of the engine.
     */
    protected void pollQueue() {
        MDC.put(MDCConstants.AGGREGATOR_ID, this.aggregatorID.toString());
        logger.info("Starting to poll queue on thread: {}", Thread.currentThread().getId());

        this.subscribe = this.createQueueObserver()
                .repeatWhen(completed -> {
                    logger.debug("Configuring queue to poll every {} milliseconds", operationsConfig.getPollingFrequency());
                    return completed.delay(operationsConfig.getPollingFrequency(), TimeUnit.MILLISECONDS);
                })
                .doOnEach(item -> logger.trace("Processing item: {}", item))
                .doOnError(error -> logger.error("Unable to complete job.", error))
                .retry()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .doOnDispose(this::resetMDC)
                .subscribe(
                        this::processJobBatch,
                        this::onError,
                        this::onCompleted
                );
    }

    protected void onError(Throwable error) {
        logger.error("Error processing queue. Exiting...", error);
        queueRunning.set(false);
        resetMDC();
    }

    protected void onCompleted() {
        logger.info("Finished processing queue. Exiting...");
        queueRunning.set(false);
        resetMDC();
    }

    /**
     * Creates an observer to monitor the queue
     */
    private Observable<Optional<JobQueueBatch>> createQueueObserver() {
        // Create using fromCallable. This ensures that no events are omitted before a subscriber connects
        return Observable.fromCallable(() -> {
            logger.trace("Polling queue for job...");
            return this.queue.claimBatch(this.aggregatorID);
        });
    }

    /**
     * Loops over the partials of a job batch and handles completed, error, and paused job scenarios
     *
     * @param job - the job to process
     */
    @Trace
    protected void processJobBatch(JobQueueBatch job) {
        this.currentBatch.set(Optional.of(job));

        final String queueCompleteTime = SplunkTimestamp.getSplunkTimestamp();
        try {
            MDC.put(MDCConstants.AGGREGATOR_ID, this.aggregatorID.toString());
            MDC.put(MDCConstants.JOB_ID, job.getJobID().toString());
            MDC.put(MDCConstants.JOB_BATCH_ID, job.getBatchID().toString());
            MDC.put(MDCConstants.ORGANIZATION_ID, job.getOrgID().toString());
            MDC.put(MDCConstants.IS_BULK, Boolean.toString(job.isBulk()));
            MDC.put(MDCConstants.IS_V2, Boolean.toString(job.isV2()));

            logger.info("Processing job, exporting to: {}.", this.operationsConfig.getExportPath());
            logger.info("dpcMetric=queueComplete,jobID={},queueCompleteTime={}",  job.getJobID(), queueCompleteTime);
            logger.debug("Has {} attributed beneficiaries", job.getPatients().size());

            Optional<String> nextPatientID = job.fetchNextPatient(aggregatorID);
            while (nextPatientID.isPresent()) {
                String patientId = nextPatientID.get();
                nextPatientID = processPatient(job, patientId);
            }

            // Finish processing the batch
            if (this.isRunning()) {
                final String jobTime = SplunkTimestamp.getSplunkTimestamp();
                // Calculate metadata for the file (length and checksum)
                calculateFileMetadata(job);
                logger.info("dpcMetric=jobComplete,completionResult={},jobID={},jobCompleteTime={}", "COMPLETE", job.getJobID(), jobTime);
                this.queue.completeBatch(job, aggregatorID);
            } else {
                logger.info("PAUSED job");
                this.queue.pauseBatch(job, aggregatorID);
            }
        } catch (Exception error) {
            try {
                final String jobTime = SplunkTimestamp.getSplunkTimestamp();
                logger.info("dpcMetric=jobFail,completionResult={},jobID={},jobCompleteTime={},failureReason={}", "FAILED", job.getJobID(), jobTime, error.getMessage());
                this.queue.failBatch(job, aggregatorID);
            } catch (Exception failedBatchException) {
                logger.error("FAILED to mark job {} batch {} as failed. Batch will remain in the running state, and stuck job logic will retry this in 15 minutes...", job.getJobID(), job.getBatchID(), failedBatchException);
            }
        } finally {
            this.currentBatch.set(Optional.empty());
        }

        // Clear the MDC before the next batch
        resetMDC();
    }

    private Optional<String> processPatient(JobQueueBatch job, String patientId) {
        jobBatchProcessor.processJobBatchPartial(aggregatorID, queue, job, patientId);

        // Stop processing when no patients or early shutdown
        return this.isRunning() ? job.fetchNextPatient(aggregatorID) : Optional.empty();
    }



    private void calculateFileMetadata(JobQueueBatch job) {
        job.getJobQueueBatchFiles()
                .forEach(batchFile -> {
                    final File file = new File(String.format("%s/%s.ndjson", this.operationsConfig.getExportPath(), batchFile.getFileName()));
                    try {
                        final byte[] checksum = AggregationUtils.generateChecksum(file);
                        batchFile.setChecksum(checksum);
                    } catch (IOException e) { // If we can't generate the checksum, that's a faulting error, just continue
                        logger.error("Unable to generate checksum for file {}", batchFile.getFileName());
                    }
                    batchFile.setFileLength(file.length());
                });
    }

    /**
     * Setup a global handler to catch the UndeliverableException case. Can be called from anywhere.
     */
    public static void setGlobalErrorHandler() {
        RxJavaPlugins.setErrorHandler(AggregationEngine::errorHandler);
    }

    /**
     * Global error handler. Needed to catch undeliverable exceptions.
     * <p>
     * See: https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling
     *
     * @param e is the exception thrown
     */
    private static void errorHandler(Throwable e) {
        logger.error("Caught exception during RxJava processing flow: ", e);

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

    public UUID getAggregatorID() {
        return aggregatorID;
    }

    protected void setSubscribe(Disposable subscribe) {
        this.subscribe = subscribe;
    }

    /**
     * Used to reset the {@link MDC} after each batch is processed.
     */
    private void resetMDC() {
        // Empty out the existing MDC and put the aggregator ID back
        MDC.setContextMap(Collections.emptyMap());
        MDC.put(MDCConstants.AGGREGATOR_ID, this.aggregatorID.toString());
    }
}
