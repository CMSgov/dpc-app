package gov.cms.dpc.fhir;

import com.google.inject.Provider;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import jakarta.inject.Inject;

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
