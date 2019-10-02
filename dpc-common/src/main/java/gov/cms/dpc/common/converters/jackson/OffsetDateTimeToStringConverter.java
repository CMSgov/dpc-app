package gov.cms.dpc.common.converters.jackson;

import com.fasterxml.jackson.databind.util.StdConverter;
import gov.cms.dpc.fhir.FHIRFormatters;

import java.time.OffsetDateTime;

/**
 * Serialize in the format expected the FHIR spec. For details see https://hl7.org/fhir/2018May/datatypes.html#instant
 */
public class OffsetDateTimeToStringConverter extends StdConverter<OffsetDateTime, String> {

    @Override
    public String convert(OffsetDateTime time) {
        return FHIRFormatters.INSTANT_FORMATTER.format(time);
    }
}
