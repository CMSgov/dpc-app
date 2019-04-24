package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Serialize in the format expected the FHIR spec. For details see https://hl7.org/fhir/2018May/datatypes.html#instant
 */
public class OffsetDateTimeToStringConverter extends StdConverter<OffsetDateTime, String> {

    private static final DateTimeFormatter fhirFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx");

    @Override
    public String convert(OffsetDateTime time) {
        return fhirFormatter.format(time);
    }
}
