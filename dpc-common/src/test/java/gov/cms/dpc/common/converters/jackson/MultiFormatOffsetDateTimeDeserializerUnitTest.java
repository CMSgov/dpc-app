package gov.cms.dpc.common.converters.jackson;

import gov.cms.dpc.fhir.FHIRFormatters;
import org.junit.jupiter.api.Test;

import javax.ws.rs.WebApplicationException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class MultiFormatOffsetDateTimeDeserializerUnitTest {

    @Test
    void testConvertIsoFormat() {
        MultiFormatOffsetDateTimeDeserializer deserializer = new MultiFormatOffsetDateTimeDeserializer();
        String validIsoTime = "2021-05-26T16:43:01.780Z";
        OffsetDateTime expected = OffsetDateTime.parse(validIsoTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        OffsetDateTime actual = deserializer.convert(validIsoTime);
        assertEquals(expected,actual, "Times should have matched");
    }

    @Test
    void testConvertFHIRFormat() {
        MultiFormatOffsetDateTimeDeserializer deserializer = new MultiFormatOffsetDateTimeDeserializer();
        String validFhirTime = "2020-03-28T18:34:07.703+00:00";
        OffsetDateTime expected = OffsetDateTime.parse(validFhirTime, FHIRFormatters.INSTANT_FORMATTER);
        OffsetDateTime actual = deserializer.convert(validFhirTime);
        assertEquals(expected,actual, "Times should have matched");
    }

    @Test
    void testInvalidFormat() {
        MultiFormatOffsetDateTimeDeserializer deserializer = new MultiFormatOffsetDateTimeDeserializer();
        String invalidTime = "2020--00-303-28T18:34:07.703+00:00";
        assertThrows(WebApplicationException.class, () -> deserializer.convert(invalidTime), "convert should have thrown exception");
    }
}