package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import com.typesafe.config.Config;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.Pair;
import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.models.JobResult;
import io.github.resilience4j.retry.RetryConfig;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.ReplaySubject;
import io.reactivex.subjects.Subject;
import io.reactivex.subjects.UnicastSubject;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.CipherOutputStream;
import javax.inject.Inject;
import java.util.ArrayList;
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

    private final String exportPath;
    private final JobQueue queue;
    private final BlueButtonClient bbclient;
    private final RetryConfig retryConfig;
    private final Config config;
    private final FhirContext fhirContext;
    private final int resourcesPerFile;
    private Disposable subscribe;
    private boolean encryptionEnabled;

    /**
     * Create an engine
     *
     * @param bbclient    - {@link BlueButtonClient } to use
     * @param queue       - {@link JobQueue} that will direct the work done
     * @param fhirContext - {@link FhirContext} for DSTU3 resources
     * @param exportPath  - The {@link ExportPath} to use for writing the output files
     * @param retryConfig - {@link RetryConfig} injected config for setting up retry handler
     */
    @Inject
    public AggregationEngine(BlueButtonClient bbclient, JobQueue queue, FhirContext fhirContext, @ExportPath String exportPath, Config config, RetryConfig retryConfig) {
        this.queue = queue;
        this.bbclient = bbclient;
        this.exportPath = exportPath;
        this.config = config;
        this.retryConfig = retryConfig;
        this.fhirContext = fhirContext;
        this.resourcesPerFile = config.hasPath("resourcesPerFile") ? config.getInt("resourcesPerFile") : 1000;
    }

    /**
     * Run the engine. Part of the Runnable interface.
     */
    @Override
    public void run() {
        // Run loop
        logger.info("Starting aggregation engine with exportPath:\"{}\"", exportPath);
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
                .subscribe(this::workExportJob, error -> logger.error("Unable to complete job.", error));
    }

    /**
     * Wrapper method for dispatching the job and handling any errors that would cause the job to fail
     *
     * @param workPair - job {@link Pair} with {@link UUID} or {@link JobModel}
     */
    private void workExportJob(Pair<UUID, JobModel> workPair) {
        final JobModel model = workPair.getRight();
        completeJob(model);
    }

    /**
     * Work a single job in the queue to completion
     *
     * @param job - the job to execute
     */
    void completeJob(JobModel job) {
        final UUID jobID = job.getJobID();
        try {
            logger.info("Processing job {}, exporting to: {}.", jobID, this.exportPath);
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
        final var fetcher = new ResourceFetcher(bbclient, retryConfig, job.getJobID(), resourceType);
        final var mixedFlow = Flowable.fromIterable(job.getPatients())
                // Fetch on parallel threads (one per CPU core)
                //.parallel()
                //.runOn(Schedulers.io())
                .flatMap(fetcher::fetchResources)
                //.sequential()
                .publish()
                .autoConnect(2);

        // Batch the non-error resources into files
        final var counter = new AtomicInteger();
        final var writer = new ResourceWriter(fhirContext, config, exportPath, job, resourceType);
        final Flowable<JobResult> resourceFlow = mixedFlow.filter(r -> r.getResourceType() != ResourceType.OperationOutcome)
                .buffer(resourcesPerFile)
                .map(batch -> writer.writeBatch(counter, batch));

        // Batch the error resources into files
        final var errorWriter = new ResourceWriter(fhirContext, config, exportPath, job, ResourceType.OperationOutcome);
        final Flowable<JobResult> outcomeFlow = mixedFlow.filter(r -> r.getResourceType() == ResourceType.OperationOutcome)
                .buffer(resourcesPerFile)
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
