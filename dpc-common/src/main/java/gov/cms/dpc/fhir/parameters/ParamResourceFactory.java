package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.fhir.annotations.FHIRParameter;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * {@link Factory} for converting {@link Parameters} to the underlying FHIR {@link org.hl7.fhir.instance.model.api.IBaseResource}
 */
public class ParamResourceFactory implements Factory<Object> {

    private static final Logger logger = LoggerFactory.getLogger(ParamResourceFactory.class);

    private final ContainerRequest request;
    private final IParser parser;
    private final Parameter parameter;

    ParamResourceFactory(ContainerRequest request, Parameter parameter, IParser parser) {
        this.request = request;
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
        try {
            return parser.parseResource(Parameters.class, request.getEntityStream());
        } catch (DataFormatException e) {
            logger.error("Unable to parse Parameters resource.", e);
            throw new WebApplicationException("Resource type must be `Parameters`", Response.Status.BAD_REQUEST);
        }
    }

    private Resource extractFHIRResource(Parameters fhirParameters) {
        final FHIRParameter annotation = parameter.getAnnotation(FHIRParameter.class);
        // Get the appropriate parameter
        final String parameterName = annotation.name();
        if (parameterName.isEmpty()) {
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
                        return new WebApplicationException(String.format("Cannot find matching parameter named `%s`", parameterName), Response.Status.BAD_REQUEST);
                    });

        }
    }
}
