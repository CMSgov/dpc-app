package gov.cms.dpc.aggregation.engine.suppression;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.aggregation.client.attribution.AttributionClient;
import gov.cms.dpc.aggregation.client.consent.ConsentClient;
import gov.cms.dpc.aggregation.exceptions.SuppressionException;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.fhir.converters.entities.ConsentEntityConverter;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

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
        // First, get the patient from the attribution service
        final Flowable<Optional<Consent>> suppressionFlow = Flowable.fromCallable(() -> this.attributionClient.fetchPatientByMBI(mbi))
                .map(Resource::getIdElement)
                .map(id -> this.consentClient.fetchConsentByMBI(id.getIdPart()))
                .map(this.throwIfSuppressed(mbi));

        return Completable.fromPublisher(suppressionFlow);
    }

    /**
     * Process an {@link Optional} {@link Consent} resource from the {@link ConsentClient} and raise an exception if the bene as opted out
     * This does not modify {@link Consent} resource and only throws if the resource is present and has an {@link ConsentEntity#OPT_OUT} policy rule
     *
     * @param beneID - {@link String} bene ID
     * @return - {@link Optional} {@link Consent} resource from the {@link ConsentClient}
     * @throws SuppressionException - if the patient has opted-out
     */
    private Function<Optional<Consent>, Optional<Consent>> throwIfSuppressed(String beneID) {
        return option -> {
            if (option.isPresent() && option.get().getPolicyRule().equals(ConsentEntityConverter.OPT_OUT_MAGIC)) {
                throw new SuppressionException(SuppressionException.SuppressionReason.OPT_OUT, beneID, "Patient has opted-out");
            }
            return option;
        };
    }
}
