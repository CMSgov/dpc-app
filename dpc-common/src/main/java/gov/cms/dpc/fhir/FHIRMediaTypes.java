package gov.cms.dpc.fhir;

import jakarta.ws.rs.core.MediaType;

public class FHIRMediaTypes {

    public static final String FHIR_JSON = "application/fhir+json";
    public static final String FHIR_NDJSON = "application/fhir+ndjson";
    public static final String APPLICATION_NDJSON = "application/ndjson";
    public static final String NDJSON = "ndjson";

    private static final MediaType FHIR_JSON_MT = MediaType.valueOf(FHIR_JSON);
    private static final MediaType FHIR_NDJSON_MT = MediaType.valueOf(FHIR_NDJSON);
    /**
     * Validates whether the given content type is of type FHIR
     * String can be null or empty
     *
     * @param mediaType - {@link String} value from {@link jakarta.ws.rs.core.HttpHeaders#CONTENT_TYPE} header.
     * @return - {@code true} content type is FHIR. {@code false} content type is not FHIR
     */
    public static boolean isFHIRContent(MediaType mediaType) {

        return mediaType.isCompatible(FHIR_JSON_MT) ||
                mediaType.isCompatible(FHIR_NDJSON_MT);
    }
}
