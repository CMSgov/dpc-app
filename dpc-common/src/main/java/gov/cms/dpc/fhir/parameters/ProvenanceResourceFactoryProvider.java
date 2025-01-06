package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import jakarta.inject.Inject;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;
import org.hl7.fhir.dstu3.model.Provenance;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.function.Function;

/**
 * Custom {@link ValueParamProvider} that lets us cleanly extract {@link org.hl7.fhir.dstu3.model.Provenance} resources from the {@link ProvenanceResourceValueFactory#PROVENANCE_HEADER}.
 */
@Provider
public class ProvenanceResourceFactoryProvider implements ValueParamProvider {

    private final Injector injector;
    private final FhirContext ctx;

    @Inject
    public ProvenanceResourceFactoryProvider(Injector injector, FhirContext ctx) {
        this.injector = injector;
        this.ctx = ctx;
    }

    @Override
    public Function<ContainerRequest, Provenance> getValueProvider(Parameter parameter) {
        if (parameter.getDeclaredAnnotation(ProvenanceHeader.class) != null && IBaseResource.class.isAssignableFrom(parameter.getRawType())) {
            return request -> new ProvenanceResourceValueFactory(injector, ctx).provide();
        }

        return null;
    }

    @Override
    public PriorityType getPriority() {
        return Priority.NORMAL;
    }
}
