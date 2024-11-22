package gov.cms.dpc.fhir.parameters;

import gov.cms.dpc.fhir.annotations.ProvenanceHeader;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;
import org.hl7.fhir.dstu3.model.Provenance;
import org.hl7.fhir.instance.model.api.IBaseResource;

import jakarta.inject.Inject;
import jakarta.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom {@link ValueParamProvider} that lets us cleanly extract {@link org.hl7.fhir.dstu3.model.Provenance} resources from the {@link ProvenanceResourceValueFactory#PROVENANCE_HEADER}.
 */
@Provider
public class ProvenanceResourceFactoryProvider implements ValueParamProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ProvenanceResourceFactoryProvider.class);
    
    private final jakarta.inject.Provider<ProvenanceResourceValueFactory> factoryProvider;

    @Inject
    public ProvenanceResourceFactoryProvider(jakarta.inject.Provider<ProvenanceResourceValueFactory> factoryProvider) {
        this.factoryProvider = factoryProvider;
        LOG.info("Created ProvenanceResourceFactoryProvider with factory provider");
    }

    @Override
    public Function<ContainerRequest, Provenance> getValueProvider(Parameter parameter) {
        LOG.info("Something has called getValueProvider with parameter " + parameter + " with declared annotation " + Arrays.toString(parameter.getDeclaredAnnotations()));

        if (parameter.getDeclaredAnnotation(ProvenanceHeader.class) != null &&
            IBaseResource.class.isAssignableFrom(parameter.getRawType())) {
            
            LOG.info("Creating value provider function for Provenance");
            return request -> {
                LOG.info("Extracting provenance from request");
                Provenance provenance = factoryProvider.get().provide();
                LOG.info("Generated provenance. Is it null? " + (provenance == null));
                if(provenance != null) {
                    LOG.info("Provenance info: " + provenance.fhirType()); 
                }
                return provenance;
            };
        }

        return null;
    }

    @Override
    public PriorityType getPriority() {
        return Priority.NORMAL;
    }
}
