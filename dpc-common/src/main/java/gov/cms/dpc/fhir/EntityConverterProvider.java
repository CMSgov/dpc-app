package gov.cms.dpc.fhir;

import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import jakarta.inject.Inject;
import jakarta.inject.Provider;


/**
 * {@link Provider} for {@link FHIREntityConverter}
 */
public class EntityConverterProvider implements Provider<FHIREntityConverter> {

    @Inject
    EntityConverterProvider() {
        // Not used
    }

    @Override
    public FHIREntityConverter get() {
        return FHIREntityConverter.initialize();
    }
}
