package gov.cms.dpc.fhir.dropwizard.handlers;

import ca.uhn.fhir.context.FhirContext;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
public class FHIRHandlerTest {

    private static FhirContext ctx = FhirContext.forDstu3();
    private static FHIRHandler handler = new FHIRHandler(ctx);

    @Nested
    @DisplayName("FHIR reader tests")
    class FHIRReaderTests {

        @Test
        void testReadable() {
            assertTrue(handler.isReadable(Group.class, null, null, MediaType.TEXT_HTML_TYPE), "Should be readable");
        }

        @Test
        void testNotReadable() {
            assertFalse(handler.isReadable(String.class, null, null, MediaType.TEXT_HTML_TYPE), "Should not be readable");
        }

        @Test
        void testFHIRRead() {
            final Patient patient = new Patient();
            patient.addIdentifier().setSystem("http://test.local").setValue("test-patient");

            final InputStream is = IOUtils.toInputStream(ctx.newJsonParser().encodeResourceToString(patient), StandardCharsets.UTF_8);
            final BaseResource p2 = handler.readFrom(BaseResource.class, null, null, MediaType.TEXT_HTML_TYPE, null, is);
            assertTrue(patient.equalsDeep(p2), "Should be equal");

        }

        @Test
        void testNonFHIRRead() {
            final InputStream is = IOUtils.toInputStream("this is not fhir", StandardCharsets.UTF_8);
            final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> handler.readFrom(BaseResource.class, null, null, MediaType.TEXT_HTML_TYPE, null, is), "Should throw exception");
            assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, exception.getResponse().getStatus(), "Should have correct error status"),
                    () -> assertEquals("HAPI-1859: Content does not appear to be FHIR JSON, first non-whitespace character was: 't' (must be '{')", exception.getMessage(), "Should have correct message"));
        }
    }

    @Nested
    @DisplayName("FHIR writer tests")
    class FHIRWriterTests {
        @Test
        void testWritable() {
            assertTrue(handler.isWriteable(Group.class, null, null, MediaType.TEXT_HTML_TYPE), "Should be writable");
        }

        @Test
        void testNotWritable() {
            assertFalse(handler.isWriteable(String.class, null, null, MediaType.TEXT_HTML_TYPE), "Should not be writable");
        }

        @Test
        void testFHIRWrite() throws IOException {
            final Patient patient = new Patient();
            patient.addIdentifier().setSystem("http://test.local").setValue("test-patient");
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();

            handler.writeTo(patient, Patient.class, null, null, MediaType.TEXT_HTML_TYPE, null, bos);
            final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            final Patient p2 = ctx.newJsonParser().parseResource(Patient.class, bis);
            assertTrue(patient.equalsDeep(p2), "Should be equal");
        }
    }
}
