package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.net.HttpHeaders;
import gov.cms.dpc.aggregation.service.*;
import gov.cms.dpc.aggregation.util.AggregationUtils;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.bluebutton.clientV2.BlueButtonClientV2;
import gov.cms.dpc.common.Constants;
import gov.cms.dpc.common.MDCConstants;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import io.reactivex.Flowable;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class JobBatchProcessor {
    private static final Logger logger = LoggerFactory.getLogger(JobBatchProcessor.class);

    private final BlueButtonClient bbclient;
    private final BlueButtonClientV2 bbclientV2;
    private final OperationsConfig operationsConfig;
    private final FhirContext fhirContext;
    private final Meter resourceMeter;
    private final Meter operationalOutcomeMeter;
    private final LookBackService lookBackService;
    private final ConsentService consentService;

    @Inject
    public JobBatchProcessor(BlueButtonClient bbclient, BlueButtonClientV2 bbclientV2, FhirContext fhirContext, MetricRegistry metricRegistry, OperationsConfig operationsConfig, LookBackService lookBackService, ConsentService consentService) {
        this.bbclient = bbclient;
        this.bbclientV2 = bbclientV2;
        this.fhirContext = fhirContext;
        this.operationsConfig = operationsConfig;
        this.lookBackService = lookBackService;
        this.consentService = consentService;

        // Metrics
        final var metricFactory = new MetricMaker(metricRegistry, JobBatchProcessor.class);
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
            flowable = Flowable.just(AggregationUtils.toOperationOutcome(OutcomeReason.CONSENT_OPTED_OUT, patientID));
        } else if (isLookBackExempt(job.getOrgID())) {
            logger.info("Skipping lookBack for org: {}", job.getOrgID().toString());
            MDC.put(MDCConstants.IS_SMOKE_TEST_ORG, "true");
            flowable = Flowable.fromIterable(job.getResourceTypes())
                    .flatMap(r -> fetchResource(job, patientID, r, job.getSince().orElse(null)));
        } else {
            List<LookBackAnswer> answers = getLookBackAnswers(job, patientID);
            if (passesLookBack(answers)) {
                flowable = Flowable.fromIterable(job.getResourceTypes())
                        .flatMap(r -> fetchResource(job, patientID, r, job.getSince().orElse(null)));
            } else {
                failReason = LookBackAnalyzer.analyze(answers);
                flowable = Flowable.just(AggregationUtils.toOperationOutcome(failReason, patientID));
            }
        }

        final var results = writeResource(job, flowable)
                .toList()
                .blockingGet();
        queue.completePartialBatch(job, aggregatorID);

        final String resourcesRequested = job.getResourceTypes().stream().map(ResourceType::getPath).filter(Objects::nonNull).collect(Collectors.joining(";"));
        final String failReasonLabel = failReason == null ? "NA" : failReason.name();
        stopWatch.stop();
        logger.info("dpcMetric=DataExportResult,dataRetrieved={},failReason={},resourcesRequested={},duration={}", failReason == null, failReasonLabel, resourcesRequested, stopWatch.getTime());
        return results;
    }

    private boolean isLookBackExempt(UUID orgId) {
        List<String> exemptOrgs = operationsConfig.getLookBackExemptOrgs();
        if (exemptOrgs != null) {
            return exemptOrgs.contains(orgId.toString());
        }
        return false;
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
    private Flowable<Resource> fetchResource(JobQueueBatch job, String patientID, ResourceType resourceType, OffsetDateTime since) {
        // Make this flow hot (ie. only called once) when multiple subscribers attach
        final var fetcher = new ResourceFetcher(bbclient,
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
            headers.put(Constants.BULK_CLIENT_ID_HEADER, job.getProviderNPI());
        } else {
            headers.put(Constants.DPC_CLIENT_ID_HEADER, job.getProviderNPI());
        }
        return headers;
    }

    private List<LookBackAnswer> getLookBackAnswers(JobQueueBatch job, String patientId) {
        List<LookBackAnswer> result = new ArrayList<>();
        final String practitionerNPI = job.getProviderNPI();
        final String organizationNPI = job.getOrgNPI();
        if (practitionerNPI != null && organizationNPI != null) {
            MDC.put(MDCConstants.PROVIDER_NPI, practitionerNPI);
            Flowable<Resource> flowable = fetchResource(job, patientId, ResourceType.ExplanationOfBenefit, null);
            result = flowable
                    .filter(resource -> ResourceType.ExplanationOfBenefit == resource.getResourceType())
                    .map(ExplanationOfBenefit.class::cast)
                    .map(resource -> lookBackService.getLookBackAnswer(resource, organizationNPI, practitionerNPI, operationsConfig.getLookBackMonths()))
                    .toList()
                    .doOnError(e -> new ArrayList<>())
                    .blockingGet();
        } else {
            logger.error("couldn't get practitionerNPI and organizationNPI from job");
        }
        return result;
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

    private Pair<Optional<List<ConsentResult>>, Optional<OperationOutcome>> getConsent(String patientId) {
        try {
            return Pair.of(consentService.getConsent(patientId), Optional.empty());
        } catch (Exception e) {
            logger.error("Unable to retrieve consent from consent service.", e);
            OperationOutcome operationOutcome = AggregationUtils.toOperationOutcome(OutcomeReason.INTERNAL_ERROR, patientId);
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

    private boolean passesLookBack(List<LookBackAnswer> answers) {
        return answers.stream()
                .anyMatch(a -> a.matchDateCriteria() && (a.orgNPIMatchAnyEobNPIs() || a.practitionerNPIMatchAnyEobNPIs()));
    }
}
