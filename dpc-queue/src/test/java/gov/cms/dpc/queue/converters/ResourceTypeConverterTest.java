package gov.cms.dpc.queue.converters;

import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceTypeConverterTest {
    private ResourceTypeListConverter converter;

    @BeforeEach
    void setup() {
        converter = new ResourceTypeListConverter();
    }

    @Test
    void goodDBFieldConversion() {
        final var result = converter.convertToEntityAttribute("Patient");
        assertTrue(result.contains(ResourceType.Patient));
    }

    @Test
    void badDBFieldConversion() {
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute("EOB"));
    }

    @Test
    void listConversion() {
        final var result = converter.convertToDatabaseColumn(List.of(ResourceType.Patient, ResourceType.ExplanationOfBenefit));
        assertEquals("Patient,ExplanationOfBenefit", result);
    }
}
