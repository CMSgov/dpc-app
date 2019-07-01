package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.utils.MetricFactory;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.Pair;
import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.models.JobResult;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The top level of the Aggregation Engine
 */
public class AggregationEngine implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);

    private final JobQueue queue;
    private final BlueButtonClient bbclient;
    private final OperationsConfig operationsConfig;
    private final FhirContext fhirContext;
    private final Meter resourceMeter;
    private final Meter operationalOutcomeMeter;
    private Disposable subscribe;

    /**
     * Create an engine
     *
     * @param bbclient    - {@link BlueButtonClient } to use
     * @param queue       - {@link JobQueue} that will direct the work done
     * @param fhirContext - {@link FhirContext} for DSTU3 resources
     * @param operationsConfig  - The {@link OperationsConfig} to use for writing the output files
      */
    @Inject
    public AggregationEngine(BlueButtonClient bbclient, JobQueue queue, FhirContext fhirContext, MetricRegistry metricRegistry, OperationsConfig operationsConfig) {
        this.queue = queue;
        this.bbclient = bbclient;
        this.fhirContext = fhirContext;
        this.operationsConfig = operationsConfig;

        final var metricFactory = new MetricFactory(metricRegistry, AggregationEngine.class);
        resourceMeter = metricFactory.registerMeter("resourceFetched");
        operationalOutcomeMeter = metricFactory.registerMeter("operationalOutcomes");
   }

    /**
     * Run the engine. Part of the Runnable interface.
     */
    @Override
    public void run() {
        // Run loop
        logger.info("Starting aggregation engine with exportPath:\"{}\" resourcesPerFile:{} parallelRequests:{} ",
                operationsConfig.getExportPath(),
                operationsConfig.getResourcesPerFileCount(),
                operationsConfig.isParallelRequestsEnabled());
        this.pollQueue();
    }

    /**
     * Stop the engine
     */
    public void stop() {
        logger.info("Shutting down aggregation engine");
        this.subscribe.dispose();
    }

    /**
     * The main run-loop of the engine
     */
    private void pollQueue() {
        setGlobalErrorHandler();
        subscribe = Observable.fromCallable(this.queue::workJob)
                .doOnNext(job -> logger.trace("Polling queue for job"))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .repeatWhen(completed -> {
                    logger.debug("No job, retrying in 2 seconds");
                    return completed.delay(2, TimeUnit.SECONDS);
                })
                .subscribe(this::completeJob, error -> logger.error("Unable to complete job.", error));
    }

    /**
     * Work a single job in the queue to completion
     *
     * @param jobPair - the job to execute
     */
    void completeJob(Pair<UUID, JobModel> jobPair) {
        final UUID jobID = jobPair.getLeft();
        final JobModel job = jobPair.getRight();
        try {
            logger.info("Processing job {}, exporting to: {}.", jobID, this.operationsConfig.getExportPath());
            logger.debug("Has {} attributed beneficiaries", job.getPatients().size());

            final var errorCounter = new AtomicInteger();
            final var results = Flowable.fromIterable(job.getResourceTypes())
                    .flatMap(resourceType -> completeResource(job, resourceType, errorCounter))
                    .toList()
                    .blockingGet();
            logger.info("COMPLETED job {}", jobID);
            this.queue.completeJob(jobID, JobStatus.COMPLETED, results);
        } catch(Exception error) {
            logger.error("FAILED job {}", jobID, error);
            this.queue.completeJob(jobID, JobStatus.FAILED, List.of());
        }
    }

    /**
     * Fetch and write a specific resource type
     * @param job context
     * @param resourceType to process
     * @param errorCounter to count the OperationalOutcome JobResults
     * @return A new job result observable
     */
    private Flowable<JobResult> completeResource(JobModel job, ResourceType resourceType, AtomicInteger errorCounter) {
        if (job.getPatients().size() == 0) {
            return Flowable.empty();
        }

        // Make this flow hot (ie. only called once)
        final var fetcher = new ResourceFetcher(bbclient, job.getJobID(), resourceType, operationsConfig);
        final Flowable<Resource> mixedFlow;
        if (operationsConfig.isParallelRequestsEnabled()) {
            mixedFlow = Flowable.fromIterable(job.getPatients())
                    .parallel()
                    .runOn(Schedulers.io())
                    .flatMap(fetcher::fetchResources)
                    .sequential();
        } else {
            mixedFlow = Flowable.fromIterable(job.getPatients()).flatMap(fetcher::fetchResources);
        }
        final var connectableMixedFlow = mixedFlow.publish().autoConnect(2);

        // Batch the non-error resources into files
        final var counter = new AtomicInteger();
        final var writer = new ResourceWriter(fhirContext, job, resourceType, operationsConfig);
        final Flowable<JobResult> resourceFlow = connectableMixedFlow.filter(r -> r.getResourceType() != ResourceType.OperationOutcome)
                .buffer(operationsConfig.getResourcesPerFileCount())
                .doOnNext(resources -> resourceMeter.mark(resources.size()))
                .map(batch -> writer.writeBatch(counter, batch));

        // Batch the error resources into files
        final var errorWriter = new ResourceWriter(fhirContext, job, ResourceType.OperationOutcome, operationsConfig);
        final Flowable<JobResult> outcomeFlow = connectableMixedFlow.filter(r -> r.getResourceType() == ResourceType.OperationOutcome)
                .buffer(operationsConfig.getResourcesPerFileCount())
                .doOnNext(outcomes -> operationalOutcomeMeter.mark(outcomes.size()))
                .map(batch -> errorWriter.writeBatch(errorCounter, batch));

        // Merge the resultant flows
        return resourceFlow.mergeWith(outcomeFlow);
    }

    /**
     * Setup a global handler to catch the UndeliverableException case.
     */
    private void setGlobalErrorHandler() {
        RxJavaPlugins.setErrorHandler(e -> {
            // Undeliverable Exceptions may happen because of parallel execution. One thread will
            // throw an exception which will cause the job to fail and (close its consumer).
            // Another thread will throw an exception as well which will be undeliverable
            if (e instanceof UndeliverableException) {
                // Merely log undeliverable exceptions
                logger.error(e.getMessage());
            } else {
                // Forward all others to current thread's uncaught exception handler
                final var thread = Thread.currentThread();
                thread.getUncaughtExceptionHandler().uncaughtException(thread, e);
            }
        });
    }
}
