package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.glassfish.hk2.api.Factory;
import org.hl7.fhir.dstu3.model.Provenance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

/**
 * {@link Factory} for extracting a {@link org.hl7.fhir.dstu3.model.Provenance} resource from a request header.
 */
public class ProvenanceResourceValueFactory implements Factory<Provenance> {

    static final String PROVENANCE_HEADER = "X-Provenance";
    private static final Logger logger = LoggerFactory.getLogger(ProvenanceResourceValueFactory.class);

    private final Provider<HttpServletRequest> requestProvider;
    private final FhirContext ctx;

    @Inject
    public ProvenanceResourceValueFactory(Provider<HttpServletRequest> requestProvider, FhirContext ctx) {
        this.requestProvider = requestProvider;
        this.ctx = ctx;
    }

    @Override
    public Provenance provide() {
        logger.info("Hey I have been asked to provide a Provenance!");

        final HttpServletRequest request = requestProvider.get();
        final String headerValue = request.getHeader(PROVENANCE_HEADER);
        
        if (headerValue == null) {
            String message = String.format("Must have %s header", PROVENANCE_HEADER);
            throw new InvalidRequestException(message);
        }

        try {
            return ctx.newJsonParser().parseResource(Provenance.class, headerValue);
        } catch (Exception e) {
            logger.error("Cannot parse Provenance", e);
            throw new InvalidRequestException("Cannot parse FHIR `Provenance` resource");
        }
    }

    @Override
    public void dispose(Provenance instance) {
        // Not used
    }
}
