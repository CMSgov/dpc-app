package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

/**
 * Custom {@link ValueFactoryProvider} that lets us cleanly extract {@link org.hl7.fhir.dstu3.model.Provenance} resources from the {@link ProvenanceResourceFactory#PROVENANCE_HEADER}.
 */
@Provider
public class ProvenanceResourceValueFactory implements ValueFactoryProvider {

    private final Injector injector;
    private final FhirContext ctx;

    @Inject
    public ProvenanceResourceValueFactory(Injector injector, FhirContext ctx) {
        this.injector = injector;
        this.ctx = ctx;
    }

    @Override
    public Factory<?> getValueFactory(Parameter parameter) {
        if (parameter.getDeclaredAnnotation(ProvenanceHeader.class) != null) {
            // If the parameter is a resource, pass it off to the resource factory
            if (IBaseResource.class.isAssignableFrom(parameter.getRawType()))
                return new ProvenanceResourceFactory(injector, ctx);
        }

        return null;
    }

    @Override
    public PriorityType getPriority() {
        return Priority.NORMAL;
    }
}
