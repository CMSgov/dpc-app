package gov.cms.dpc.common.converters.jackson;

import com.fasterxml.jackson.databind.util.StdConverter;
import gov.cms.dpc.fhir.FHIRFormatters;
import jakarta.ws.rs.BadRequestException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Deserialize from the format expected in the FHIR spec. For details see https://hl7.org/fhir/2018May/datatypes.html#instant
 */
public class MultiFormatOffsetDateTimeDeserializer extends StdConverter<String, OffsetDateTime> {

    private static final DateTimeFormatter[] supportedFormats = {FHIRFormatters.INSTANT_FORMATTER, DateTimeFormatter.ISO_OFFSET_DATE_TIME};

    @Override
    public OffsetDateTime convert(String s) {

        for(DateTimeFormatter formatter:supportedFormats){
            try {
                return OffsetDateTime.parse(s, formatter);
            }catch (DateTimeParseException e){
                //Ignore exception and try next format
            }
        }
        throw new BadRequestException(String.format("Could not parse date: %s",s));
    }
}
