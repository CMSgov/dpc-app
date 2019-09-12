package gov.cms.dpc.common.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import gov.cms.dpc.fhir.FHIRFormatters;

import java.time.OffsetDateTime;

/**
 * Deserialize from the format expected in the FHIR spec. For details see https://hl7.org/fhir/2018May/datatypes.html#instant
 */
public class StringToOffsetDateTimeConverter extends StdConverter<String, OffsetDateTime> {

    @Override
    public OffsetDateTime convert(String s) {
        return OffsetDateTime.parse(s, FHIRFormatters.INSTANT_FORMATTER);
    }
}
