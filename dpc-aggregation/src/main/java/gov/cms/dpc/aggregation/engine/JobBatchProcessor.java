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
import jakarta.inject.Inject;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.*;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


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
            Pair<Flowable<Resource>, OutcomeReason> lookBackResult = checkLookBack(optPatient.get(), job);
                flowable = Optional.of(lookBackResult.getLeft());  // if passing, list of EOBs
                failReason = lookBackResult.getRight() == null ?
                        Optional.empty() : Optional.of(lookBackResult.getRight());
        }

        // All checks passed, load resources
        if(failReason.isEmpty()) {
            Flowable<Resource> coverageFlow = Flowable.empty();
            if (job.getResourceTypes().contains(DPCResourceType.Coverage)) {
                coverageFlow = fetchResource(job, optPatient.get(), DPCResourceType.Coverage, job.getSince().orElse(null));
            }

            Flowable<Resource> resultFlowable = Flowable.empty();
            Map<DPCResourceType, Flowable<Resource>> resourceFlowables = Map.of(
                    DPCResourceType.Patient, Flowable.just(optPatient.get()),
                    DPCResourceType.ExplanationOfBenefit, flowable.get(),
                    DPCResourceType.Coverage, coverageFlow
            );
            for (DPCResourceType jobType : job.getResourceTypes()) {
                resultFlowable = Flowable.concat(resultFlowable, resourceFlowables.get(jobType));
            }

            Date sinceParam = job.getSince().isPresent() ?
                    Date.from(job.getSince().get().toInstant()) : Date.from(Instant.EPOCH);
            flowable = Optional.of(
                    resultFlowable.filter(r -> r.getMeta().getLastUpdated() == null
                                            || r.getMeta().getLastUpdated().after(sinceParam))
            );
        }

        final var results = writeResource(job, flowable.get())
                .toList()
                .blockingGet();
        queue.completePartialBatch(job, aggregatorID);

        AtomicReference<String> fileSize = new AtomicReference<>("");
        results.forEach(file -> {
                    if (file.getResourceType() != null) {
                        fileSize.set(fileSize + file.getResourceType().name() + ":" + file.getPatientFileSize() + ";");
                    }
                });

        double durationInSeconds = stopWatch.getDuration().getSeconds() + ((double) stopWatch.getDuration().getNano() / 1000000000);
        final String failReasonLabel = failReason.map(Enum::name).orElse("NA");
        stopWatch.stop();
        String patientId = optPatient.isPresent() ? optPatient.get().getId() : "-1";
        logger.info("dpcMetric=DataExportResult,PatientId={}, AggregatorId={}, dataRetrieved={},failReason={},duration={} , resourceFileSizes={}",
                patientId, aggregatorID,failReason.isEmpty(), failReasonLabel, durationInSeconds,fileSize.get());
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
     * If the look back check passes, a pair of a Flowable of {@link ExplanationOfBenefit} as Resource objects and a null OutcomeReason.
     */
    private Pair<Flowable<Resource>, OutcomeReason> checkLookBack(Patient patient, JobQueueBatch job) {
        Pair<List<LookBackAnswer>, Flowable<Resource>> lookBackPair = getLookBackAnswers(job, patient);
        List<LookBackAnswer> answers = lookBackPair.getLeft();
        Flowable<Resource> eobs = lookBackPair.getRight();

        if (isLookBackExempt(job.getOrgID())) {
            logger.info("Skipping lookBack for org: {}", job.getOrgID());
            MDC.put(MDCConstants.IS_SMOKE_TEST_ORG, "true");
        } else {
            if (!passesLookBack(answers)) {
                OutcomeReason failReason = LookBackAnalyzer.analyze(answers);
                return Pair.of(
                    Flowable.just(
                        AggregationUtils.toOperationOutcome(
                            failReason,
                            patient.getId(),
                            OperationOutcome.IssueType.SUPPRESSED
                        )
                    ),
                    failReason
                );
            }
        }

        // Passes lookback check or is exempt, return Explanations of Benefit and no fail reason
        return Pair.of(eobs, null);
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
                job.getTransactionTime(),
                operationsConfig.getFetchWarnThresholdSeconds());
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
        if (patients.getEntry().size() == 1) {
            Patient patient = (Patient) patients.getEntryFirstRep().getResource();
            MDC.put(MDCConstants.PATIENT_FHIR_ID, patient.getIdPart());
            return Optional.of(patient);
        }

        logger.error("Expected 1 Patient to match MBI but found {}", patients.getEntry().size());
        return Optional.empty();
    }

    private Pair<List<LookBackAnswer>, Flowable<Resource>> getLookBackAnswers(JobQueueBatch job, Patient patient) {
        List<LookBackAnswer> result = new ArrayList<>();
        Flowable<Resource> eobs = Flowable.empty();
        final String practitionerNPI = job.getProviderNPI();
        final String organizationNPI = job.getOrgNPI();
        if (practitionerNPI != null && organizationNPI != null) {
            MDC.put(MDCConstants.PROVIDER_NPI, practitionerNPI);
            Flowable<Resource> flowable = fetchResource(job, patient, DPCResourceType.ExplanationOfBenefit, null);
            eobs = flowable.filter(resource -> Objects.requireNonNull(DPCResourceType.ExplanationOfBenefit.getPath()).equals(resource.getResourceType().getPath()));
            result = eobs
                    .map(ExplanationOfBenefit.class::cast)
                    .map(resource -> lookBackService.getLookBackAnswer(resource, organizationNPI, practitionerNPI, operationsConfig.getLookBackMonths()))
                    .toList()
                    .doOnError(e -> new ArrayList<>())
                    .blockingGet();
        } else {
            logger.error("couldn't get practitionerNPI and organizationNPI from job");
        }
        return Pair.of(result, eobs);
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
                    return groupedByResourceFlow.compose(upstream -> bufferAndWrite(upstream, writer, resourceCount, sequenceCount));
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
            final ConsentResult latestConsent = Collections.max(consentResults, Comparator.comparing(ConsentResult::getConsentDate));
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
