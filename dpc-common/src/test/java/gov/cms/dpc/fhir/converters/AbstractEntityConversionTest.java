package gov.cms.dpc.fhir.converters;

import gov.cms.dpc.common.entities.PatientEntity;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
