package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.net.HttpHeaders;
import com.google.inject.name.Named;
import gov.cms.dpc.aggregation.service.ConsentResult;
import gov.cms.dpc.aggregation.service.ConsentService;
import gov.cms.dpc.aggregation.util.AggregationUtils;
import gov.cms.dpc.bluebutton.clientV2.BlueButtonClientV2;
import gov.cms.dpc.common.Constants;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import io.reactivex.Flowable;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class JobBatchProcessorV2 {
    private static final Logger logger = LoggerFactory.getLogger(JobBatchProcessorV2.class);

    private final BlueButtonClientV2 bbclientV2;
    private final OperationsConfig operationsConfig;
    private final FhirContext fhirContext;
    private final Meter resourceMeter;
    private final Meter operationalOutcomeMeter;
    private final ConsentService consentService;

    @Inject
    public JobBatchProcessorV2(BlueButtonClientV2 bbclientV2, @Named("fhirContextR4") FhirContext fhirContext, MetricRegistry metricRegistry, OperationsConfig operationsConfig, ConsentService consentService) {
        this.bbclientV2 = bbclientV2;
        this.fhirContext = fhirContext;
        this.operationsConfig = operationsConfig;
        this.consentService = consentService;

        // Metrics
        final var metricFactory = new MetricMaker(metricRegistry, JobBatchProcessorV2.class);
        resourceMeter = metricFactory.registerMeter("resourceFetched");
        operationalOutcomeMeter = metricFactory.registerMeter("operationalOutcomes");
    }

    /**
     * Processes a partial of a job batch. Marks the partial as completed upon processing
     *
     * @param aggregatorID the current aggregatorID
     * @param queue        the queue
     * @param job          the job to process
     * @param patientID    the current patient id to process
     * @return A list of batch files {@link JobQueueBatchFile}
     */
    public List<JobQueueBatchFile> processJobBatchPartial(UUID aggregatorID, IJobQueue queue, JobQueueBatch job, String patientID) {
        StopWatch stopWatch = StopWatch.createStarted();
        OutcomeReason failReason = null;
        final Pair<Optional<List<ConsentResult>>, Optional<OperationOutcome>> consentResult = getConsent(patientID);

        Flowable<Resource> flowable;
        if (consentResult.getRight().isPresent()) {
            flowable = Flowable.just(consentResult.getRight().get());
            failReason = OutcomeReason.INTERNAL_ERROR;
        } else if (isOptedOut(consentResult.getLeft())) {
            failReason = OutcomeReason.CONSENT_OPTED_OUT;
            flowable = Flowable.just(AggregationUtils.toOperationOutcomeV2(OutcomeReason.CONSENT_OPTED_OUT, patientID));
        } else {
            logger.info("Skipping lookBack for V2 job: {}", job.getOrgID().toString());
            flowable = Flowable.fromIterable(job.getResourceTypes())
                    .flatMap(r -> fetchResource(job, patientID, r, job.getSince().orElse(null)));
        }

        final var results = writeResource(job, flowable)
                .toList()
                .blockingGet();
        queue.completePartialBatch(job, aggregatorID);

        final String resourcesRequested = job.getResourceTypes().stream().map(DPCResourceType::getPath).filter(Objects::nonNull).collect(Collectors.joining(";"));
        final String failReasonLabel = failReason == null ? "NA" : failReason.name();
        stopWatch.stop();
        logger.info("dpcMetric=DataExportResult,dataRetrieved={},failReason={},resourcesRequested={},duration={}", failReason == null, failReasonLabel, resourcesRequested, stopWatch.getTime());
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
    private Flowable<Resource> fetchResource(JobQueueBatch job, String patientID, DPCResourceType resourceType, OffsetDateTime since) {
        // Make this flow hot (ie. only called once) when multiple subscribers attach
        final var fetcher = new ResourceFetcherV2(bbclientV2,
                job.getJobID(),
                job.getBatchID(),
                resourceType,
                since,
                job.getTransactionTime());
        return fetcher.fetchResources(patientID, buildHeaders(job))
                .flatMap(Flowable::fromIterable);
    }

    private Map<String, String> buildHeaders(JobQueueBatch job) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.X_FORWARDED_FOR, job.getRequestingIP());
        headers.put(Constants.BFD_ORIGINAL_QUERY_ID_HEADER, job.getJobID().toString());
        if (job.isBulk()) {
            headers.put(Constants.BULK_JOB_ID_HEADER, job.getJobID().toString());
            headers.put(Constants.BULK_CLIENT_ID_HEADER, job.getRequestUrl());
        } else {
            headers.put(Constants.DPC_CLIENT_ID_HEADER, job.getRequestUrl());
        }
        return headers;
    }

    private Flowable<JobQueueBatchFile> writeResource(JobQueueBatch job, Flowable<Resource> flow) {
        return flow.groupBy(Resource::getResourceType)
                .flatMap(groupedByResourceFlow -> {
                    final var resourceCount = new AtomicInteger();
                    final var sequenceCount = new AtomicInteger();
                    final var resourceType = groupedByResourceFlow.getKey();
                    final var dpcResourceType = DPCResourceType.valueOf(resourceType != null ? resourceType.toString() : null);
                    job.getJobQueueFileLatest(dpcResourceType).ifPresent(file -> {
                        resourceCount.set(file.getCount());
                        sequenceCount.set(file.getSequence());
                    });
                    final var writer = new ResourceWriterV2(fhirContext, job, dpcResourceType, operationsConfig);
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
    private Publisher<JobQueueBatchFile> bufferAndWrite(Flowable<Resource> upstream, ResourceWriterV2 writer, AtomicInteger resourceCount, AtomicInteger sequenceCount) {
        final Flowable<Resource> filteredUpstream = upstream.filter(r -> r.getResourceType().getPath().equals(writer.getResourceType().getPath()));
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

    private Flowable<JobQueueBatchFile> writeResources(Flowable<Resource> upstream, ResourceWriterV2 writer, AtomicInteger sequenceCount, Meter meter) {
        return upstream
                .buffer(operationsConfig.getResourcesPerFileCount())
                .doOnNext(outcomes -> meter.mark(outcomes.size()))
                .map(batch -> writer.writeBatch(sequenceCount, batch));
    }

    private Meter getMeter(DPCResourceType resourceType) {
        return DPCResourceType.OperationOutcome == resourceType ? operationalOutcomeMeter : resourceMeter;
    }

    private Pair<Optional<List<ConsentResult>>, Optional<OperationOutcome>> getConsent(String patientId) {
        try {
            return Pair.of(consentService.getConsent(patientId), Optional.empty());
        } catch (Exception e) {
            logger.error("Unable to retrieve consent from consent service.", e);
            OperationOutcome operationOutcome = AggregationUtils.toOperationOutcomeV2(OutcomeReason.INTERNAL_ERROR, patientId);
            return Pair.of(Optional.empty(), Optional.of(operationOutcome));
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private boolean isOptedOut(Optional<List<ConsentResult>> consentResultsOptional) {
        if (consentResultsOptional.isPresent()) {
            final List<ConsentResult> consentResults = consentResultsOptional.get();
            long optOutCount = consentResults.stream().filter(consentResult -> {
                final boolean isActive = consentResult.isActive();
                final boolean isOptOut = ConsentResult.PolicyType.OPT_OUT.equals(consentResult.getPolicyType());
                final boolean isFutureConsent = consentResult.getConsentDate().after(new Date());
                return isActive && isOptOut && !isFutureConsent;
            }).count();
            return optOutCount > 0;
        }
        return true;
    }

}
