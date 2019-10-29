package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import gov.cms.dpc.fhir.annotations.FHIRParameter;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.model.Parameter;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Resource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;

/**
 * {@link Factory} for converting {@link Parameters} to the underlying FHIR {@link org.hl7.fhir.instance.model.api.IBaseResource}
 */
public class ParamResourceFactory implements Factory<Object> {

    private final Injector injector;
    private final FhirContext ctx;
    private final Parameter parameter;

    ParamResourceFactory(Injector injector, Parameter parameter, FhirContext ctx) {
        this.injector = injector;
        this.ctx = ctx;
        this.parameter = parameter;
    }

    @Override
    public Object provide() {
        // Directly call the injector to get the current Servlet Request.
        // It would be better to have this automatically provided, but it's simple enough to do it manually, rather than wrangling Guice scopes.
        final HttpServletRequest request = injector.getInstance(HttpServletRequest.class);
        final Parameters parameters;
        try {
            parameters = ctx.newJsonParser().parseResource(Parameters.class, request.getInputStream());
        } catch (IOException e) {
            throw new WebApplicationException("Cannot parse input stream", e);
        }
        final Resource resource;
        final FHIRParameter annotation = parameter.getAnnotation(FHIRParameter.class);
        // Get the appropriate parameter
        if (annotation.name().equals("")) {
            resource = parameters.getParameterFirstRep().getResource();
        } else {
            resource = parameters
                    .getParameter()
                    .stream()
                    .filter(param -> param.hasName() && param.getName().equals(annotation.name()))
                    .map(Parameters.ParametersParameterComponent::getResource)
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Cannot find matching parameter"));
        }
        return parameter.getRawType().cast(resource);
    }

    @Override
    public void dispose(Object instance) {
        // Not used
    }
}
