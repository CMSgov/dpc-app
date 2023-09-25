package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.fhir.annotations.FHIRParameter;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
import java.util.function.Function;

/**
 * Custom {@link ValueParamProvider} that lets us cleanly map between {@link org.hl7.fhir.dstu3.model.Parameters} and use specified resource types.
 */
@Provider
public class FHIRParamValueFactory implements ValueParamProvider {

    private final FhirContext ctx;

    @Inject
    FHIRParamValueFactory(FhirContext ctx) {
        this.ctx = ctx;
    }


    @Override
    public Function<ContainerRequest, Object> getValueProvider(Parameter parameter) {
        if (parameter.getDeclaredAnnotation(FHIRParameter.class) != null) {
            // If the parameter is a resource, pass it off to the resource factory
            if (IBaseResource.class.isAssignableFrom(parameter.getRawType()))
                return request -> new ParamResourceFactory(request, parameter, ctx.newJsonParser()).provide();
        }

        return null;
    }

    @Override
    public PriorityType getPriority() {
        return Priority.NORMAL;
    }
}
