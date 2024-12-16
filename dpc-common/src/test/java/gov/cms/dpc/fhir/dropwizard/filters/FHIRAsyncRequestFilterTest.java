package gov.cms.dpc.fhir.dropwizard.filters;

import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.util.List;

import static gov.cms.dpc.fhir.FHIRHeaders.PREFER_HEADER;
import static gov.cms.dpc.fhir.FHIRHeaders.PREFER_RESPOND_ASYNC;
import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(BufferedLoggerHandler.class)
public class FHIRAsyncRequestFilterTest {

    private static FHIRAsyncRequestFilter filter = new FHIRAsyncRequestFilter();
    private static ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);

    @BeforeAll
    static void setup() {
        Mockito.reset(context);
    }

    @Nested
    @DisplayName("Test `Accepts` header")
    class AcceptsHeader {

        @Test
        void testMissingAcceptsHeader() {
            final MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            Mockito.when(context.getHeaders()).thenReturn(map);
            final BadRequestException exception = assertThrows(BadRequestException.class, () -> filter.filter(context));
            assertEquals("'application/fhir+json' is the only supported response format", exception.getMessage(), "Should have correct message");
        }

        @Test
        void testCorrectAcceptsHeader() {
            final MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            map.put(HttpHeaders.ACCEPT, List.of(FHIR_JSON));
            map.put(PREFER_HEADER, List.of(PREFER_RESPOND_ASYNC));
            Mockito.when(context.getHeaders()).thenReturn(map);
            filter.filter(context);
        }

        @Test
        void testMultipleAcceptsHeader() {
            final MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            map.put(HttpHeaders.ACCEPT, List.of("wrong", FHIR_JSON));
            map.put(PREFER_HEADER, List.of(PREFER_RESPOND_ASYNC));
            Mockito.when(context.getHeaders()).thenReturn(map);
            filter.filter(context);
        }

        @Test
        void testIncorrectAcceptsHeader() {
            final MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            map.put(HttpHeaders.ACCEPT, List.of("This is not right"));
            Mockito.when(context.getHeaders()).thenReturn(map);
            final BadRequestException exception = assertThrows(BadRequestException.class, () -> filter.filter(context));
            assertEquals("'application/fhir+json' is the only supported response format", exception.getMessage(), "Should have correct message");
        }
    }

    @Nested
    @DisplayName("Test `Prefer` header")
    class PreferHeader {
        @Test
        void testMissingPreferHeader() {
            final MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            map.put(HttpHeaders.ACCEPT, List.of(FHIR_JSON));
            Mockito.when(context.getHeaders()).thenReturn(map);
            final BadRequestException exception = assertThrows(BadRequestException.class, () -> filter.filter(context));
            assertEquals("One 'Prefer' header is required with a 'respond-async' value", exception.getMessage(), "Should have correct message");
        }

        @Test
        void testCorrectPreferHeader() {
            final MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            map.put(HttpHeaders.ACCEPT, List.of(FHIR_JSON));
            map.put(PREFER_HEADER, List.of(PREFER_RESPOND_ASYNC));
            Mockito.when(context.getHeaders()).thenReturn(map);
            filter.filter(context);
        }

        @Test
        void testMultiplePreferHeader() {
            final MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            map.put(HttpHeaders.ACCEPT, List.of(FHIR_JSON));
            map.put(PREFER_HEADER, List.of("wrong", PREFER_RESPOND_ASYNC));
            Mockito.when(context.getHeaders()).thenReturn(map);
            final BadRequestException exception = assertThrows(BadRequestException.class, () -> filter.filter(context), "Should throw exception");
            assertEquals("One 'Prefer' header is required with a 'respond-async' value", exception.getMessage(), "Should have correct message");
        }

        @Test
        void testIncorrectPreferHeader() {
            final MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
            map.put(HttpHeaders.ACCEPT, List.of(FHIR_JSON));
            map.put(PREFER_HEADER, List.of("wrong prefer"));
            Mockito.when(context.getHeaders()).thenReturn(map);
            final BadRequestException exception = assertThrows(BadRequestException.class, () -> filter.filter(context));
            assertEquals("One 'Prefer' header is required with a 'respond-async' value", exception.getMessage(), "Should have correct message");
        }
    }
}
