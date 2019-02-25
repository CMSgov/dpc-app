package gov.cms.dpc.fhir;

import javax.ws.rs.core.MediaType;

public class FHIRMediaTypes {

    public static final String FHIR_JSON = "application/fhir+json";
    public static final String FHIR_NDJSON = "application/fhir+ndjson";

    private static final MediaType FHIR_JSON_MT = MediaType.valueOf(FHIR_JSON);
    private static final MediaType FHIR_NDJSON_MT = MediaType.valueOf(FHIR_NDJSON);

    /**
     * Validates whether or not the given content type is of type FHIR
     * String can be null or empty
     * @param mediaType - {@link String} value from {@link javax.ws.rs.core.HttpHeaders#CONTENT_TYPE} header.
     * @return - {@code true} content type is FHIR. {@code false} content type is not FHIR
     */
    public static boolean isFHIRContent(MediaType mediaType) {

        return mediaType.isCompatible(MediaType.valueOf(FHIR_JSON)) ||
                mediaType.isCompatible(MediaType.valueOf(FHIR_NDJSON));
    }
}
