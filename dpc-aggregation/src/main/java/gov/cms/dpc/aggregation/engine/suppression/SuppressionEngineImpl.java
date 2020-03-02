package gov.cms.dpc.aggregation.engine.suppression;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.aggregation.client.attribution.AttributionClient;
import gov.cms.dpc.aggregation.client.consent.ConsentClient;
import gov.cms.dpc.aggregation.exceptions.SuppressionException;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.fhir.converters.entities.ConsentEntityConverter;
import io.reactivex.Completable;
import io.reactivex.functions.Function;
import org.hl7.fhir.dstu3.model.Consent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class SuppressionEngineImpl implements SuppressionEngine {

    private static final Logger logger = LoggerFactory.getLogger(SuppressionEngineImpl.class);

    private final AttributionClient attributionClient;
    private final ConsentClient consentClient;

    @Inject
    SuppressionEngineImpl(AttributionClient attributionClient, ConsentClient consentClient, FhirContext ctx) {
        this.attributionClient = attributionClient;
        this.consentClient = consentClient;
    }


    @Override
    public Completable processSuppression(String mbi) {
        logger.debug("Processing suppression for MBI: {}", mbi);
        // Dispatch checks to both attribution and consent services, we race them because they happen independently

        // TODO: This is superfluous until DPC-651 is worked on, but I'm leaving it here as a placeholder.
        // It mostly works to ensure the patient hasn't been deleted before we access data for it
        final Completable attributionCompletable = this.attributionClient.fetchPatientByMBI(mbi)
                .flatMapCompletable(patient -> Completable.complete());

        // Query the consent service to determine if the patient has opted-out
        final Completable consentCompletable = this.consentClient.fetchConsentByMBI(mbi)
                .map(this.throwIfSuppressed(mbi))
                .flatMapCompletable(c -> Completable.complete());

        return Completable.concatArray(attributionCompletable, consentCompletable);
    }

    /**
     * Process a {@link Consent} resource from the {@link ConsentClient} and raise an exception if the bene as opted out
     * This does not modify {@link Consent} resource and only throws if the resource is present and has an {@link ConsentEntity#OPT_OUT} policy rule
     *
     * @param beneID - {@link String} bene ID
     * @return - {@link Consent} resource from the {@link ConsentClient}
     * @throws SuppressionException - if the patient has opted-out
     */
    private Function<Consent, Consent> throwIfSuppressed(String beneID) {
        return consent -> {
            if (consent.getPolicyRule().equals(ConsentEntityConverter.OPT_OUT_MAGIC)) {
                throw new SuppressionException(SuppressionException.SuppressionReason.OPT_OUT, beneID, "Patient has opted-out");
            }
            return consent;
        };
    }
}
