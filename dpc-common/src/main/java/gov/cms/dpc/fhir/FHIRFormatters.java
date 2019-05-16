package gov.cms.dpc.fhir;

import java.time.format.DateTimeFormatter;

public class FHIRFormatters {

    /**
     * {@link DateTimeFormatter} which outputs in a format parsable by {@link org.hl7.fhir.dstu3.model.InstantType}.
     * See: https://www.hl7.org/fhir/datatypes.html#instant
     */
    public static final DateTimeFormatter INSTANT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx");

    /**
     * {@link DateTimeFormatter} which outputs in a format parsable by {@link org.hl7.fhir.dstu3.model.DateTimeType}.
     * See: https://www.hl7.org/fhir/datatypes.html#dateTime
     */
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private FHIRFormatters() {
        // Not used
    }
}
