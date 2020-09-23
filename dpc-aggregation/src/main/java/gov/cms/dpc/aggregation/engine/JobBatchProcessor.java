package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.aggregation.service.LookBackAnswer;
import gov.cms.dpc.aggregation.service.LookBackService;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import io.reactivex.Flowable;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class JobBatchProcessor {

    private final BlueButtonClient bbclient;
    private final OperationsConfig operationsConfig;
    private final FhirContext fhirContext;
    private final Meter resourceMeter;
    private final Meter operationalOutcomeMeter;

    @Inject
    public JobBatchProcessor(BlueButtonClient bbclient, FhirContext fhirContext, MetricRegistry metricRegistry, OperationsConfig operationsConfig) {
        this.bbclient = bbclient;
        this.fhirContext = fhirContext;
        this.operationsConfig = operationsConfig;

        // Metrics
        final var metricFactory = new MetricMaker(metricRegistry, JobBatchProcessor.class);
        resourceMeter = metricFactory.registerMeter("resourceFetched");
        operationalOutcomeMeter = metricFactory.registerMeter("operationalOutcomes");
    }

    /**
     * Processes a partial of a job batch. Marks the partial as completed upon processing
     *
     * @param aggregatorID  the current aggregatorID
     * @param queue         the queue
     * @param job           the job to process
     * @param patientID     the current patient id to process
     * @param lookBackAnswers
     * @return A list of batch files {@link JobQueueBatchFile}
     */
    public List<JobQueueBatchFile> processJobBatchPartial(UUID aggregatorID, IJobQueue queue, JobQueueBatch job, String patientID, List<LookBackAnswer> lookBackAnswers) {
        boolean matched = lookBackAnswers.stream()
                .anyMatch(a -> a.matchDateCriteria() && (a.orgNPIMatchAnyEobNPIs() || a.practitionerNPIMatchAnyEobNPIs()));

        Flowable<Resource> flowable = matched ?
                Flowable.fromIterable(job.getResourceTypes())
                        .flatMap(r -> fetchResource(job, patientID, r, job.getSince().orElse(null)))
                :
                Flowable.just(LookBackService.getOperationOutcome(lookBackAnswers, patientID));

        final var results = writeResource(job, flowable)
                .toList()
                .blockingGet();
        queue.completePartialBatch(job, aggregatorID);
        return results;
    }

    /**
     * Fetch and write a specific resource type
     *
     * @param job          the job to associate the fetch
     * @param patientID    the patientID to fetch data
     * @param resourceType the resourceType to fetch data
     * @param since        the since date
     * @return A flowable and resourceType the user requested
     */
    public Flowable<Resource> fetchResource(JobQueueBatch job, String patientID, ResourceType resourceType, OffsetDateTime since) {
        // Make this flow hot (ie. only called once) when multiple subscribers attach
        final var fetcher = new ResourceFetcher(bbclient,
                job.getJobID(),
                job.getBatchID(),
                resourceType,
                since,
                job.getTransactionTime());
        return fetcher.fetchResources(patientID)
                .flatMap(Flowable::fromIterable);
    }

    private Flowable<JobQueueBatchFile> writeResource(JobQueueBatch job, Flowable<Resource> flow) {
        return flow.groupBy(Resource::getResourceType)
                .flatMap(groupedByResourceFlow -> {
                    final var resourceCount = new AtomicInteger();
                    final var sequenceCount = new AtomicInteger();
                    final var resourceType = groupedByResourceFlow.getKey();
                    job.getJobQueueFileLatest(resourceType).ifPresent(file -> {
                        resourceCount.set(file.getCount());
                        sequenceCount.set(file.getSequence());
                    });
                    final var writer = new ResourceWriter(fhirContext, job, resourceType, operationsConfig);
                    return groupedByResourceFlow.compose((upstream) -> bufferAndWrite(upstream, writer, resourceCount, sequenceCount));
                });
    }


    /**
     * This part of the flow chain buffers resources and writes them in batches to a file
     *
     * @param writer        - the writer to use
     * @param resourceCount - the number of resources in the current file
     * @param sequenceCount - the sequence counter
     * @return a transformed flow
     */
    private Publisher<JobQueueBatchFile> bufferAndWrite(Flowable<Resource> upstream, ResourceWriter writer, AtomicInteger resourceCount, AtomicInteger sequenceCount) {
        final Flowable<Resource> filteredUpstream = upstream.filter(r -> r.getResourceType() == writer.getResourceType());
        final var connectableMixedFlow = filteredUpstream.publish().autoConnect(2);

        var resourcesInCurrentFileCount = resourceCount.getAndSet(0);
        var resourcesPerFile = operationsConfig.getResourcesPerFileCount();
        var firstResourceBatchCount = resourcesInCurrentFileCount < resourcesPerFile ? resourcesPerFile - resourcesInCurrentFileCount : resourcesPerFile;

        if (resourcesInCurrentFileCount == resourcesPerFile) {
            // Start a new file since the file has been filled up
            sequenceCount.incrementAndGet();
        }
        Meter meter = getMeter(writer.getResourceType());
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

    private Meter getMeter(ResourceType resourceType) {
        return ResourceType.OperationOutcome == resourceType ? operationalOutcomeMeter : resourceMeter;
    }
}
