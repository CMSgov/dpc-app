package gov.cms.dpc.fhir.paramtests;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import gov.cms.dpc.fhir.annotations.FHIRParameter;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

@Provider
public class ParamValueFactory implements ValueFactoryProvider {

    private final Injector injector;
    private final FhirContext ctx;

    @Inject
    ParamValueFactory(Injector injector, FhirContext ctx) {
        this.injector = injector;
        this.ctx = ctx;
    }


    @Override
    public Factory<?> getValueFactory(Parameter parameter) {
        if (parameter.getDeclaredAnnotation(FHIRParameter.class) != null) {
            return new ParamFactory(injector, parameter, ctx);
        }

        return null;
    }

    @Override
    public PriorityType getPriority() {
        return Priority.NORMAL;
    }
}
