package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.aggregation.service.*;
import gov.cms.dpc.aggregation.util.AggregationUtils;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.MDCConstants;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import io.reactivex.Flowable;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.*;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static gov.cms.dpc.fhir.FHIRExtractors.getPatientMBI;
import static gov.cms.dpc.fhir.FHIRExtractors.getPatientMBIs;

public class JobBatchProcessor {
    private static final Logger logger = LoggerFactory.getLogger(JobBatchProcessor.class);

    private final BlueButtonClient bbclient;
    private final OperationsConfig operationsConfig;
    private final FhirContext fhirContext;
    private final Meter resourceMeter;
    private final Meter operationalOutcomeMeter;
    private final LookBackService lookBackService;
    private final ConsentService consentService;

    @Inject
    public JobBatchProcessor(BlueButtonClient bbclient, FhirContext fhirContext, MetricRegistry metricRegistry, OperationsConfig operationsConfig, LookBackService lookBackService, ConsentService consentService) {
        this.bbclient = bbclient;
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
     * @param mbi          the current patient mbi to process
     * @return A list of batch files {@link JobQueueBatchFile}
     */
    public List<JobQueueBatchFile> processJobBatchPartial(UUID aggregatorID, IJobQueue queue, JobQueueBatch job, String mbi) {
        StopWatch stopWatch = StopWatch.createStarted();
        Optional<OutcomeReason> failReason = Optional.empty();
        Optional<Flowable<Resource>> flowable = Optional.empty();

        // Load the Patient resource from BFD.
        final Optional<Patient> optPatient = fetchPatient(job, mbi);
        if(optPatient.isEmpty()) {
            // Failed to load patient
            failReason = Optional.of(OutcomeReason.INTERNAL_ERROR);
            flowable = Optional.of(Flowable.just(AggregationUtils.toOperationOutcome(failReason.get(), mbi)));
        }

        // Check if the patient has opted out
        if(flowable.isEmpty()) {
            Optional<Pair<Flowable<Resource>, OutcomeReason>> consentResult = checkForOptOut(optPatient.get());
            if(consentResult.isPresent()) {
                flowable = Optional.of(consentResult.get().getLeft());
                failReason = Optional.of(consentResult.get().getRight());
            }
        }

        // Check if the patient passes look back
        if(flowable.isEmpty()) {
            Optional<Pair<Flowable<Resource>, OutcomeReason>> lookBackResult = checkLookBack(optPatient.get(), job);
            if(lookBackResult.isPresent()) {
                flowable = Optional.of(lookBackResult.get().getLeft());
                failReason = Optional.of(lookBackResult.get().getRight());
            }
        }

        // All checks passed, load resources
        if(flowable.isEmpty()) {
            flowable = Optional.of(
                    Flowable.fromIterable(job.getResourceTypes()).flatMap(r -> fetchResource(job, optPatient.get(), r, job.getSince().orElse(null)))
            );
        }

        final var results = writeResource(job, flowable.get())
                .toList()
                .blockingGet();
        queue.completePartialBatch(job, aggregatorID);

        final String resourcesRequested = job.getResourceTypes().stream().map(DPCResourceType::getPath).filter(Objects::nonNull).collect(Collectors.joining(";"));
        final String failReasonLabel = failReason.isEmpty() ? "NA" : failReason.get().name();
        stopWatch.stop();
        logger.info("dpcMetric=DataExportResult,dataRetrieved={},failReason={},resourcesRequested={},duration={}", failReason.isEmpty(), failReasonLabel, resourcesRequested, stopWatch.getTime());
        return results;
    }

    /**
     * Checks the given patient against the consent service and returns any issues if the check doesn't pass.
     * @param patient   {@link Patient} resource we're checking consent for.
     * @return If there's a problem, it returns a pair of a {@link Flowable} {@link OperationOutcome} and an {@link OutcomeReason}.
     * If the Patient passes the consent check, it returns an empty {@link Optional}s.
     */
    private Optional<Pair<Flowable<Resource>, OutcomeReason>> checkForOptOut(Patient patient) {
        final Pair<Optional<List<ConsentResult>>, Optional<OperationOutcome>> consentResult = getConsent(patient);

        if (consentResult.getRight().isPresent()) {
            // Consent check returned an error
            return Optional.of(
                    Pair.of(
                        Flowable.just(consentResult.getRight().get()),
                        OutcomeReason.INTERNAL_ERROR
                    )
            );
        } else if (isOptedOut(consentResult.getLeft())) {
            // Enrollee is opted out
            return Optional.of(
                    Pair.of(
                            Flowable.just(AggregationUtils.toOperationOutcome(OutcomeReason.CONSENT_OPTED_OUT, FHIRExtractors.getPatientMBI(patient))),
                            OutcomeReason.CONSENT_OPTED_OUT
                    )
            );
        }

        // Passes consent check
        return Optional.empty();
    }

    /**
     * Does the patient look back check and returns any issues if it doesn't pass.
     * @param patient   {@link Patient} resource we're looking for a relationship for.
     * @param job       {@link JobQueueBatch} currently running.
     * @return If there's a problem, it returns a pair of a {@link Flowable} {@link OperationOutcome} and an {@link OutcomeReason}.
     * If the look back check passes, an empty {@link Optional}.
     */
    private Optional<Pair<Flowable<Resource>, OutcomeReason>> checkLookBack(Patient patient, JobQueueBatch job) {
        if (isLookBackExempt(job.getOrgID())) {
            logger.info("Skipping lookBack for org: {}", job.getOrgID().toString());
            MDC.put(MDCConstants.IS_SMOKE_TEST_ORG, "true");
        } else {
            List<LookBackAnswer> answers = getLookBackAnswers(job, patient);
            if (!passesLookBack(answers)) {
                OutcomeReason failReason = LookBackAnalyzer.analyze(answers);
                return Optional.of(
                        Pair.of(
                        Flowable.just(AggregationUtils.toOperationOutcome(failReason, FHIRExtractors.getPatientMBI(patient))),
                        failReason
                        )
                );
            }
        }

        // Passes lookback check
        return Optional.empty();
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
     * @param job       the job to associate the fetch
     * @param patient   the {@link Patient} we're fetching data for
     * @return A flowable and resourceType the user requested
     */
    private Flowable<Resource> fetchResource(JobQueueBatch job, Patient patient, DPCResourceType resourceType, OffsetDateTime since) {
        // Make this flow hot (ie. only called once) when multiple subscribers attach
        final var fetcher = new ResourceFetcher(bbclient,
                job.getJobID(),
                job.getBatchID(),
                resourceType,
                since,
                job.getTransactionTime());
        return fetcher.fetchResources(patient, new JobHeaders(job.getRequestingIP(),job.getJobID().toString(),
                        job.getProviderNPI(),job.getTransactionTime().toString(),job.isBulk()).buildHeaders())
                           .flatMap(Flowable::fromIterable);
    }

    /**
     * Fetches the {@link Patient} referenced by the given mbi.  Throws a {@link ResourceNotFoundException} if no
     * {@link Patient} can be found.
     * @param job   The job associated to the fetch
     * @param mbi   The mbi of the {@link Patient}
     * @return      The {@link Patient}
     */
    private Optional<Patient> fetchPatient(JobQueueBatch job, String mbi) {
        JobHeaders headers = new JobHeaders(
                job.getRequestingIP(),
                job.getJobID().toString(),
                job.getProviderNPI(),
                job.getTransactionTime().toString(),
                job.isBulk());

        Bundle patients;
        try {
            patients = bbclient.requestPatientFromServerByMbi(mbi, headers.buildHeaders());
        } catch (Exception e) {
            logger.error("Failed to retrieve Patient", e);
            return Optional.empty();
        }

        // If we get more than one unique Patient for an MBI then we've got some upstream problems.
        if (patients.getTotal() == 1) {
            return Optional.of((Patient) patients.getEntryFirstRep().getResource());
        }

        logger.error("Expected 1 Patient to match MBI but found {}", patients.getTotal());
        return Optional.empty();
    }

    private List<LookBackAnswer> getLookBackAnswers(JobQueueBatch job, Patient patient) {
        List<LookBackAnswer> result = new ArrayList<>();
        final String practitionerNPI = job.getProviderNPI();
        final String organizationNPI = job.getOrgNPI();
        if (practitionerNPI != null && organizationNPI != null) {
            MDC.put(MDCConstants.PROVIDER_NPI, practitionerNPI);
            Flowable<Resource> flowable = fetchResource(job, patient, DPCResourceType.ExplanationOfBenefit, null);
            result = flowable
                    .filter(resource -> Objects.requireNonNull(DPCResourceType.ExplanationOfBenefit.getPath()).equals(resource.getResourceType().getPath()))
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
                    final var dpcResourceType = DPCResourceType.valueOf(resourceType != null ? resourceType.toString() : null);
                    job.getJobQueueFileLatest(dpcResourceType).ifPresent(file -> {
                        resourceCount.set(file.getCount());
                        sequenceCount.set(file.getSequence());
                    });
                    final var writer = new ResourceWriter(fhirContext, job, dpcResourceType, operationsConfig);
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

    private Flowable<JobQueueBatchFile> writeResources(Flowable<Resource> upstream, ResourceWriter writer, AtomicInteger sequenceCount, Meter meter) {
        return upstream
                .buffer(operationsConfig.getResourcesPerFileCount())
                .doOnNext(outcomes -> meter.mark(outcomes.size()))
                .map(batch -> writer.writeBatch(sequenceCount, batch));
    }

    private Meter getMeter(DPCResourceType resourceType) {
        return DPCResourceType.OperationOutcome == resourceType ? operationalOutcomeMeter : resourceMeter;
    }

    /**
     * Returns a {@link List} of {@link ConsentResult}s if successful.  An {@link OperationOutcome} if not.  Only one of
     * the two {@link Optional}s returned will be filled in.
     *
     * @param patient   A {@link Patient} that we want to get {@link ConsentResult}s for
     * @return          A {@link Pair}
     */
    private Pair<Optional<List<ConsentResult>>, Optional<OperationOutcome>> getConsent(Patient patient) {
        try {
            return Pair.of(consentService.getConsent(getPatientMBIs(patient)), Optional.empty());
        } catch (Exception e) {
            logger.error("Unable to retrieve consent from consent service.", e);
            OperationOutcome operationOutcome = AggregationUtils.toOperationOutcome(OutcomeReason.INTERNAL_ERROR, getPatientMBI(patient));
            return Pair.of(Optional.empty(), Optional.of(operationOutcome));
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private boolean isOptedOut(Optional<List<ConsentResult>> consentResultsOptional) {
        if (consentResultsOptional.isPresent()) {
            final List<ConsentResult> consentResults = consentResultsOptional.get();
            if (consentResults.isEmpty()) {
                return false;
            }
            final ConsentResult latestConsent = Collections.max(consentResults, Comparator.comparing(consent -> consent.getConsentDate()));
            final boolean isActive = latestConsent.isActive();
            final boolean isOptOut = ConsentResult.PolicyType.OPT_OUT.equals(latestConsent.getPolicyType());
            final boolean isFutureConsent = latestConsent.getConsentDate().after(new Date());
            return isActive && isOptOut && !isFutureConsent;
        }
        // This should never execute. Log an error.
        logger.error("Consent result is unexpectedly null.");
        return true;
    }

    private boolean passesLookBack(List<LookBackAnswer> answers) {
        return answers.stream()
                .anyMatch(a -> a.matchDateCriteria() && (a.orgNPIMatchAnyEobNPIs() || a.practitionerNPIMatchAnyEobNPIs()));
    }
}
