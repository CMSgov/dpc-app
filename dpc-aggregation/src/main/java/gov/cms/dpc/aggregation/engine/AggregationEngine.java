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
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.Subject;
import io.reactivex.subjects.UnicastSubject;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        subscribe = Observable.fromCallable(this.queue::workJob)
                .doOnNext(job -> logger.trace("Polling queue for job"))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .repeatWhen(completed -> {
                    logger.debug("No job, retrying in 2 seconds");
                    return completed.delay(2, TimeUnit.SECONDS);
                })
                .doOnError(e -> logger.error("Error", e))
                .subscribe(this::workExportJob,
                        error -> logger.error("Unable to complete job.", error));
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
        logger.info("Processing job {}, exporting to: {}.", jobID, this.exportPath);

        List<String> attributedBeneficiaries = job.getPatients();
        logger.debug("Has {} attributed beneficiaries", attributedBeneficiaries.size());

        final var writer = new ResourceWriter(fhirContext, config, exportPath, job, ResourceType.OperationOutcome);
        final var errorCounter = new AtomicInteger();
        final var errorSubject = UnicastSubject.<Resource>create().toSerialized();
        final Observable<JobResult> errorResult = errorSubject
                .buffer(resourcesPerFile)
                .map(batch -> writer.writeBatch(errorCounter, batch));

        final var results = new ArrayList<JobResult>();
        Observable.fromIterable(job.getResourceTypes())
                .flatMap(resourceType -> completeResource(job, resourceType, errorSubject))
                .doFinally(errorSubject::onComplete)
                .concatWith(errorResult)
                .blockingSubscribe(
                        // onNext
                        results::add,
                        // onError
                        error -> {
                            logger.error("FAILED job {}", jobID, error);
                            this.queue.completeJob(jobID, JobStatus.FAILED, List.of());
                        },
                        // onComplete
                        () -> {
                            logger.info("COMPLETED job {}", jobID);
                            this.queue.completeJob(jobID, JobStatus.COMPLETED, results);
                        });

    }

    /**
     * Fetch and write a specific resource type
     * @param job context
     * @param resourceType to process
     * @param errorSubject to record errors from Blue Button
     * @return A new job result observable
     */
    private Observable<JobResult> completeResource(JobModel job, ResourceType resourceType, Subject<Resource> errorSubject) {
        final var counter = new AtomicInteger();
        final var fetcher = new ResourceFetcher(bbclient, retryConfig, job.getJobID(), resourceType, errorSubject);
        final var writer = new ResourceWriter(fhirContext, config, exportPath, job, resourceType);
        return Observable.fromIterable(job.getPatients())
                .subscribeOn(Schedulers.io())
                .flatMap(fetcher::fetchResources)
                .concatMap(fetcher::unpackBundles)
                .buffer(resourcesPerFile)
                .map(batch -> writer.writeBatch(counter, batch));
    }
}
