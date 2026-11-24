package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import jakarta.inject.Inject;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.function.Function;

/**
 * Custom {@link ValueParamProvider} that lets us cleanly extract Provenance resources from the {@link ProvenanceResourceValueFactory#PROVENANCE_HEADER}.
 */
@Provider
public class ProvenanceResourceFactoryProvider implements ValueParamProvider {

    private final Injector injector;
    private final FhirContext stu3Ctx;
    private final FhirContext r4Ctx;

    @Inject
    public ProvenanceResourceFactoryProvider(Injector injector,
                                             @Named("stu3") FhirContext stu3Ctx,
                                             @Named("r4") FhirContext r4Ctx) {
        this.injector = injector;
        this.stu3Ctx = stu3Ctx;
        this.r4Ctx = r4Ctx;
    }

    @Override
    public Function<ContainerRequest, IBaseResource> getValueProvider(Parameter parameter) {
        if (parameter.getDeclaredAnnotation(ProvenanceHeader.class) == null || !IBaseResource.class.isAssignableFrom(parameter.getRawType())) {
            return null;
        }

        // If the parameter is a Provenance resource, pass it off to the resource factory
        final Class<?> declaredType = parameter.getRawType();
        if (declaredType.equals(org.hl7.fhir.dstu3.model.Provenance.class)) {
            return request -> new ProvenanceResourceValueFactory(injector, stu3Ctx, org.hl7.fhir.dstu3.model.Provenance.class).provide();
        }
        if (declaredType.equals(org.hl7.fhir.r4.model.Provenance.class)) {
            return request -> new ProvenanceResourceValueFactory(injector, r4Ctx, org.hl7.fhir.r4.model.Provenance.class).provide();
        }

        // Declared type is some other IBaseResource with @ProvenanceHeader - not supported.
        return null;
    }

    @Override
    public PriorityType getPriority() {
        return Priority.NORMAL;
    }
}
