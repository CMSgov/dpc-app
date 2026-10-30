package gov.cms.dpc.fhir.parameters.r4;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.name.Named;
import gov.cms.dpc.fhir.annotations.r4.FHIRParameter;
import jakarta.inject.Inject;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;
import org.hl7.fhir.r4.model.BaseResource;

import java.util.function.Function;

/**
 * Custom {@link ValueParamProvider} that lets us cleanly map between {@link org.hl7.fhir.r4.model.Parameters} and use specified resource types.
 */
@Provider
public class FHIRParamValueFactory implements ValueParamProvider {

    private final FhirContext ctx;

    @Inject
    public FHIRParamValueFactory(@Named("r4") FhirContext ctx) {
        this.ctx = ctx;
    }


    @Override
    public Function<ContainerRequest, Object> getValueProvider(Parameter parameter) {
        // If the parameter is a resource, pass it off to the resource factory
        if (parameter.getDeclaredAnnotation(FHIRParameter.class) != null && BaseResource.class.isAssignableFrom(parameter.getRawType())) {
            return request -> new ParamResourceFactory(request, parameter, ctx.newJsonParser()).provide();
        }

        return null;
    }

    @Override
    public PriorityType getPriority() {
        return Priority.NORMAL;
    }
}
