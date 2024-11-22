package gov.cms.dpc.fhir.converters;

import org.junit.jupiter.api.BeforeEach;

import java.util.List;

public abstract class AbstractEntityConversionTest {

    protected FHIREntityConverter converter;

    @BeforeEach
    void setup() {
        final List<FHIRConverter<?, ?>> converters = registerConverters();

        if (converters.isEmpty()) {
            converter = FHIREntityConverter.initialize();
        } else {
            converter = FHIREntityConverter.initialize(converters);
        }

    }

    protected abstract List<FHIRConverter<?, ?>> registerConverters();
}
