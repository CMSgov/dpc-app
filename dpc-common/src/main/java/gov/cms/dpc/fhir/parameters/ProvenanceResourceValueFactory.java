package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.api.Factory;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Factory} for extracting a Provenance resource from a request header.
 */
public class ProvenanceResourceValueFactory implements Factory<IBaseResource> {

    static final String PROVENANCE_HEADER = "X-Provenance";
    private static final Logger logger = LoggerFactory.getLogger(ProvenanceResourceValueFactory.class);

    private final Injector injector;
    private final FhirContext ctx;
    private final Class<? extends IBaseResource> provenanceClass;

    ProvenanceResourceValueFactory(Injector injector, FhirContext ctx, Class<? extends IBaseResource> provenanceClass) {
        this.injector = injector;
        this.ctx = ctx;
        this.provenanceClass = provenanceClass;
    }

    @Override
    public IBaseResource provide() {
        final HttpServletRequest request = injector.getInstance(HttpServletRequest.class);
        final String headerValue = request.getHeader(PROVENANCE_HEADER);
        if (headerValue == null) {
            throw new WebApplicationException(String.format("Must have %s header", PROVENANCE_HEADER), Response.Status.BAD_REQUEST);
        }
        final IBaseResource provenance;
        try {
            provenance = ctx.newJsonParser().parseResource(provenanceClass, headerValue);
        } catch (Exception e) {
            logger.error("Cannot parse Provenance", e);
            throw new WebApplicationException("Cannot parse FHIR `Provenance` resource", Response.Status.BAD_REQUEST);
        }

        return provenance;
    }

    @Override
    public void dispose(IBaseResource instance) {
        // Not used
    }
}
