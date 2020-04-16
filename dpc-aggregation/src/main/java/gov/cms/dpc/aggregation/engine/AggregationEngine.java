package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.newrelic.api.agent.Trace;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.annotations.AggregatorID;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final LookBackService lookBackService;
    private final UUID aggregatorID;
    private final IJobQueue queue;
    private final BlueButtonClient bbclient;
    private final OperationsConfig operationsConfig;
    private final FhirContext fhirContext;
    private final Meter resourceMeter;
    private final Meter operationalOutcomeMeter;
    private Disposable subscribe;

    /**
     * The initial value is set to true so when the aggregation instance starts up,
     * it's not in an unhealthy state (determined by the AggregationEngineHealthCheck)
     */
    protected AtomicBoolean queueRunning = new AtomicBoolean(true);

    /**
     * Create an engine.
     *
     * @param aggregatorID     - The ID of the current working aggregator
     * @param bbclient         - {@link BlueButtonClient } to use
     * @param queue            - {@link IJobQueue} that will direct the work done
     * @param fhirContext      - {@link FhirContext} for DSTU3 resources
     * @param metricRegistry   - {@link MetricRegistry} for metrics
     * @param operationsConfig - The {@link OperationsConfig} to use for writing the output files
     */
    @Inject
    public AggregationEngine(@AggregatorID UUID aggregatorID, BlueButtonClient bbclient, IJobQueue queue, FhirContext fhirContext, MetricRegistry metricRegistry, OperationsConfig operationsConfig, LookBackService lookBackService) {
        this.aggregatorID = aggregatorID;
        this.queue = queue;
        this.bbclient = bbclient;
        this.fhirContext = fhirContext;
        this.operationsConfig = operationsConfig;
        this.lookBackService = lookBackService;

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
        queueRunning.set(true);
        this.pollQueue();
    }

    /**
     * Stop the engine.
     */
    public void stop() {
        logger.info("Shutting down aggregation engine");
        queueRunning.set(false);
        this.subscribe.dispose();
    }

    public Boolean isRunning() {
        return queueRunning.get();
    }

    /**
     * The main run-loop of the engine.
     */
    protected void pollQueue() {
        this.subscribe = this.createQueueObserver()
                .repeatWhen(completed -> {
                    logger.debug(String.format("Configuring queue to poll every %d milliseconds", operationsConfig.getPollingFrequency()));
                    return completed.delay(operationsConfig.getPollingFrequency(), TimeUnit.MILLISECONDS);
                })
                .doOnEach(item -> logger.trace("Processing item: " + item.toString()))
                .doOnError(error -> logger.error("Unable to complete job.", error))
                .retry()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .subscribe(
                        this::processJobBatch,
                        this::onError,
                        this::onCompleted
                );
    }

    protected void onError(Throwable error) {
        logger.error("Error processing queue. Exiting...", error);
        queueRunning.set(false);
    }

    protected void onCompleted() {
        logger.info("Finished processing queue. Exiting...");
        queueRunning.set(false);
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
        try {
            logger.info("Processing job {} batch {}, exporting to: {}.", job.getJobID(), job.getBatchID(), this.operationsConfig.getExportPath());
            logger.debug("Has {} attributed beneficiaries", job.getPatients().size());

            Optional<String> nextPatientID = job.fetchNextPatient(aggregatorID);

            // Stop processing when no patients or early shutdown
            while (nextPatientID.isPresent()) {
                String patientId = nextPatientID.get();
                if (lookBackService.associatedWithRoster(job.getOrgID(), job.getProviderID(), patientId)) {
                    Pair<Flowable<List<Resource>>, ResourceType> pair = completeResource(job, patientId, ResourceType.ExplanationOfBenefit);
                    Boolean hasClaims = pair.getLeft()
                            .flatMap(Flowable::fromIterable)
                            .filter(resource -> pair.getRight() == resource.getResourceType() || ResourceType.OperationOutcome == resource.getResourceType())
                            .any(resource -> resource.getResourceType() == ResourceType.OperationOutcome || lookBackService.hasClaimWithin((ExplanationOfBenefit) resource, job.getOrgID(), job.getProviderID(), operationsConfig.getLookBackMonths()))
                            .onErrorReturn((error) -> false)
                            .blockingGet();
                    if (Boolean.TRUE.equals(hasClaims)) {
                        this.processJobBatchPartial(job, patientId);
                    }
                }

                // Check if the subscriber is still running before getting the next part of the batch
                nextPatientID = this.isRunning() ? job.fetchNextPatient(aggregatorID) : Optional.empty();
            }

            // Finish processing the batch
            if (this.isRunning()) {
                logger.info("COMPLETED job {} batch {}", job.getJobID(), job.getBatchID());
                // Calculate metadata for the file (length and checksum)
                calculateFileMetadata(job);
                this.queue.completeBatch(job, aggregatorID);
            } else {
                logger.info("PAUSED job {} batch {}", job.getJobID(), job.getBatchID());
                this.queue.pauseBatch(job, aggregatorID);
            }
        } catch (Exception error) {
            try {
                logger.error("FAILED job {} batch {}", job.getJobID(), job.getBatchID(), error);
                this.queue.failBatch(job, aggregatorID);
            } catch (Exception failedBatchException) {
                logger.error("FAILED to mark job {} batch {} as failed. Batch will remain in the running state, and stuck job logic will retry this in 5 minutes...", job.getJobID(), job.getBatchID(), failedBatchException);
            }
        }
    }

    private void calculateFileMetadata(JobQueueBatch job) {
        job.getJobQueueBatchFiles()
                .forEach(batchFile -> {
                    final File file = new File(String.format("%s/%s.ndjson", this.operationsConfig.getExportPath(), batchFile.getFileName()));
                    try {
                        final byte[] checksum = generateChecksum(file);
                        batchFile.setChecksum(checksum);
                    } catch (IOException e) { // If we can't generate the checksum, that's a faulting error, just continue
                        logger.error("Unable to generate checksum for file {}", batchFile.getFileName());
                    }
                    batchFile.setFileLength(file.length());
                });
    }

    /**
     * Processes a partial of a job batch. Marks the partial as completed upon processing
     *
     * @param job       - the job to process
     * @param patientID - The current patient id processing
     */
    private List<JobQueueBatchFile> processJobBatchPartial(JobQueueBatch job, String patientID) {
        final var results = Flowable.fromIterable(job.getResourceTypes())
                .map(resourceType -> completeResource(job, patientID, resourceType))
                .flatMap(result -> writeResource(job, result.getRight(), result.getLeft().flatMap(Flowable::fromIterable)))
                .toList()
                .blockingGet(); // Wait on the main thread until completion
        this.queue.completePartialBatch(job, aggregatorID);
        return results;
    }

    /**
     * Fetch and write a specific resource type
     *
     * @param job          context
     * @param resourceType to process
     */
    private Pair<Flowable<List<Resource>>, ResourceType> completeResource(JobQueueBatch job, String patientID, ResourceType resourceType) {
        // Make this flow hot (ie. only called once) when multiple subscribers attach
        final var fetcher = new ResourceFetcher(bbclient, job.getJobID(), job.getBatchID(), resourceType, operationsConfig);
        return Pair.of(fetcher.fetchResources(patientID), resourceType);
    }

    private Flowable<JobQueueBatchFile> writeResource(JobQueueBatch job, ResourceType resourceType, Flowable<Resource> flow) {
        var connectableMixedFlow = flow.publish().autoConnect(2);
        // Batch the non-error resources into files
        final var resourceCount = new AtomicInteger();
        final var sequenceCount = new AtomicInteger();
        job.getJobQueueFileLatest(resourceType).ifPresent(file -> {
            resourceCount.set(file.getCount());
            sequenceCount.set(file.getSequence());
        });
        final var writer = new ResourceWriter(fhirContext, job, resourceType, operationsConfig);
        final Flowable<JobQueueBatchFile> resourceFlow = connectableMixedFlow.compose((upstream) -> bufferAndWrite(upstream, writer, resourceCount, sequenceCount, resourceMeter));

        // Batch the error resources into files
        final var errorResourceCount = new AtomicInteger();
        final var errorSequenceCount = new AtomicInteger();
        job.getJobQueueFileLatest(ResourceType.OperationOutcome).ifPresent(file -> {
            errorResourceCount.set(file.getCount());
            errorSequenceCount.set(file.getSequence());
        });
        final var errorWriter = new ResourceWriter(fhirContext, job, ResourceType.OperationOutcome, operationsConfig);
        final Flowable<JobQueueBatchFile> outcomeFlow = connectableMixedFlow.compose((upstream) -> bufferAndWrite(upstream, errorWriter, errorResourceCount, errorSequenceCount, operationalOutcomeMeter));

        // Merge the resultant flows
        return resourceFlow.mergeWith(outcomeFlow);
    }

    /**
     * This part of the flow chain buffers resources and writes them in batches to a file
     *
     * @param writer        - the writer to use
     * @param resourceCount - the number of resources in the current file
     * @param sequenceCount - the sequence counter
     * @param meter         - a meter on the number of resources
     * @return a transformed flow
     */
    private Publisher<JobQueueBatchFile> bufferAndWrite(Flowable<Resource> upstream, ResourceWriter writer, AtomicInteger resourceCount, AtomicInteger sequenceCount, Meter meter) {
        final Flowable<Resource> filteredUpstream = upstream.filter(r -> r.getResourceType() == writer.getResourceType());
        final var connectableMixedFlow = filteredUpstream.publish().autoConnect(2);

        var resourcesInCurrentFileCount = resourceCount.getAndSet(0);
        var resourcesPerFile = operationsConfig.getResourcesPerFileCount();
        var firstResourceBatchCount = resourcesInCurrentFileCount < resourcesPerFile ? resourcesPerFile - resourcesInCurrentFileCount : resourcesPerFile;

        if (resourcesInCurrentFileCount == resourcesPerFile) {
            // Start a new file since the file has been filled up
            sequenceCount.incrementAndGet();
        }

        // Handle the scenario where a previous file was already written by breaking up the flow into the first batch and the buffered batch
        final Flowable<JobQueueBatchFile> partialBatch = connectableMixedFlow
                .compose(stream -> writeResources(stream.take(firstResourceBatchCount), writer, sequenceCount, meter));
        final Flowable<JobQueueBatchFile> bufferedBatch = connectableMixedFlow
                .compose(stream -> writeResources(stream.skip(firstResourceBatchCount), writer, sequenceCount, meter));

        return partialBatch.mergeWith(bufferedBatch);
    }

    private Flowable<JobQueueBatchFile> writeResources(Flowable<Resource> upstream, ResourceWriter writer, AtomicInteger sequenceCount, Meter meter) {
        return upstream
                .buffer(operationsConfig.getResourcesPerFileCount())
                .doOnNext(outcomes -> meter.mark(outcomes.size()))
                .map(batch -> writer.writeBatch(sequenceCount, batch));
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

    static byte[] generateChecksum(File file) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return new SHA256.Digest().digest(fileInputStream.readAllBytes());
        }
    }
}
