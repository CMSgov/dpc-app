package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import com.google.inject.Injector;
import gov.cms.dpc.fhir.annotations.FHIRParameter;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.model.Parameter;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * {@link Factory} for converting {@link Parameters} to the underlying FHIR {@link org.hl7.fhir.instance.model.api.IBaseResource}
 */
public class ParamResourceFactory implements Factory<Object> {

    private static final Logger logger = LoggerFactory.getLogger(ParamResourceFactory.class);

    private final Injector injector;
    private final IParser parser;
    private final Parameter parameter;

    ParamResourceFactory(Injector injector, Parameter parameter, IParser parser) {
        this.injector = injector;
        this.parser = parser;
        this.parameter = parameter;
    }

    @Override
    public Object provide() {
        final Resource resource = extractFHIRResource(extractParameters());
        final Class<?> rawType = parameter.getRawType();
        try {
            return rawType.cast(resource);
        } catch (ClassCastException e) {
            logger.error("Parameter type does not match payload", e);
            throw new WebApplicationException(String.format("Provided resource must be: `%s`, not `%s`", rawType.getSimpleName(), resource.getResourceType()), Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void dispose(Object instance) {
        // Not used
    }

    private Parameters extractParameters() {
        // Directly call the injector to get the current Servlet Request.
        // It would be better to have this automatically provided, but it's simple enough to do it manually, rather than wrangling Guice scopes.
        final HttpServletRequest request = injector.getInstance(HttpServletRequest.class);
        try {
            return parser.parseResource(Parameters.class, request.getInputStream());
        } catch (DataFormatException e) {
            logger.error("Unable to parse Parameters resource.", e);
            throw new WebApplicationException("Resource type must be `Parameters`", Response.Status.BAD_REQUEST);
        } catch (IOException e) {
            throw new WebApplicationException("Cannot read input stream", e);
        }
    }

    private Resource extractFHIRResource(Parameters fhirParameters) {
        final FHIRParameter annotation = parameter.getAnnotation(FHIRParameter.class);
        // Get the appropriate parameter
        final String parameterName = annotation.name();
        if (parameterName.equals("")) {
            return fhirParameters.getParameterFirstRep().getResource();
        } else {
            return fhirParameters
                    .getParameter()
                    .stream()
                    .filter(param -> param.hasName() && param.getName().equals(parameterName))
                    .map(Parameters.ParametersParameterComponent::getResource)
                    .findAny()
                    .orElseThrow(() -> {
                        logger.error("Cannot find parameter named `{}` in resource", parameterName);
                        throw new WebApplicationException(String.format("Cannot find matching parameter named `%s`", parameterName), Response.Status.BAD_REQUEST);
                    });

        }
    }
}
