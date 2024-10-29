package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.inject.Injector;
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

    private final Injector injector;
    private final FhirContext ctx;

    ProvenanceResourceValueFactory(Injector injector, FhirContext ctx) {
        this.injector = injector;
        this.ctx = ctx;
    }

    @Override
    public Provenance provide() {
        final HttpServletRequest request = injector.getInstance(HttpServletRequest.class);
        final String headerValue = request.getHeader(PROVENANCE_HEADER);
        
        if (headerValue == null) {
            String message = String.format("Must have %s header", PROVENANCE_HEADER);
            throw new InvalidRequestException(message);
        }
        final Provenance provenance;
        try {
            provenance = ctx.newJsonParser().parseResource(Provenance.class, headerValue);
        } catch (Exception e) {
            logger.error("Cannot parse Provenance", e);
            throw new InvalidRequestException("Cannot parse FHIR `Provenance` resource");
        }

        return provenance;
    }

    @Override
    public void dispose(Provenance instance) {
        // Not used
    }
}
