package gov.cms.dpc.fhir;

import com.google.inject.Inject;
import com.google.inject.Provider;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;


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
