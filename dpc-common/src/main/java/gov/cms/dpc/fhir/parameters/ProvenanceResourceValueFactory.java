package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import org.hl7.fhir.dstu3.model.Provenance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.function.Supplier;

/**
 * {@link Supplier} for extracting a {@link org.hl7.fhir.dstu3.model.Provenance} resource from a request header.
 */
public class ProvenanceResourceValueFactory implements Supplier<Provenance> {

    static final String PROVENANCE_HEADER = "X-Provenance";
    private static final Logger logger = LoggerFactory.getLogger(ProvenanceResourceValueFactory.class);

    private final Injector injector;
    private final FhirContext ctx;

    ProvenanceResourceValueFactory(Injector injector, FhirContext ctx) {
        this.injector = injector;
        this.ctx = ctx;
    }

    @Override
    public Provenance get() {
        final HttpServletRequest request = injector.getInstance(HttpServletRequest.class);
        final String headerValue = request.getHeader(PROVENANCE_HEADER);
        if (headerValue == null) {
            throw new WebApplicationException(String.format("Must have %s header", PROVENANCE_HEADER), Response.Status.BAD_REQUEST);
        }
        final Provenance provenance;
        try {
            provenance = ctx.newJsonParser().parseResource(Provenance.class, headerValue);
        } catch (Exception e) {
            logger.error("Cannot parse Provenance", e);
            throw new WebApplicationException("Cannot parse FHIR `Provenance` resource", Response.Status.BAD_REQUEST);
        }

        return provenance;
    }
}
