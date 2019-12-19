package gov.cms.dpc.fhir.dropwizard.handlers;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(DropwizardExtensionsSupport.class)
public class BundleHandlerTest {

    private static ResourceExtension resource = buildResource();
    private static FhirContext ctx;

    private static ResourceExtension buildResource() {
        ctx = FhirContext.forDstu3();
        final FHIRHandler fhirHandler = new FHIRHandler(ctx);
        return ResourceExtension
                .builder()
                .addProvider(fhirHandler)
                .addProvider(new BundleHandler(fhirHandler))
                .addResource(new BundleTestResource())
                .build();
    }

    @Test
    void testBundle() {
        final String bundleString = resource
                .target("")
                .request()
                .get(String.class);

        final Bundle bundle = ctx.newJsonParser().parseResource(Bundle.class, bundleString);
        assertAll(() -> assertEquals(1, bundle.getTotal(), "Should have a single entry"),
                () -> assertEquals(Patient.class, bundle.getEntryFirstRep().getResource().getClass(), "Should be a patient"),
                () -> assertEquals("Patient/test-patient", bundle.getEntryFirstRep().getResource().getId(), "Should have the correct id"));
    }

    @Test
    void testEmptyBundle() {
        final String bundleString = resource
                .target("/empty")
                .request()
                .get(String.class);

        final Bundle bundle = ctx.newJsonParser().parseResource(Bundle.class, bundleString);
        assertEquals(0, bundle.getTotal(), "Should be an empty bundle");
    }

    @Test
    void testStringListReturn() {
        //noinspection Convert2Diamond - Removing the class assertion causes javac to explode.
        final List<String> strings = resource
                .target("/strings")
                .request()
                .get(new GenericType<List<String>>() {
                });

        assertAll(() -> assertEquals(1, strings.size(), "Should have a single member"),
                () -> assertEquals("Not a valid resource", strings.get(0), "String value should be equal"));
    }

    @Test
    void testStringReturn() {

        final String s = resource
                .target("/string")
                .request()
                .get(String.class);

        assertEquals("Not a resource either", s, "Should have string");
    }


    @Path("/")
    @Produces(FHIRMediaTypes.FHIR_JSON)
    public static class BundleTestResource {

        @GET
        public List<Patient> returnPatientList() {
            final Patient p = new Patient();
            p.setId("test-patient");
            p.setGender(Enumerations.AdministrativeGender.MALE);
            return Collections.singletonList(p);
        }

        @GET
        @Path("/empty")
        public List<Practitioner> returnEmptyList() {
            return Collections.emptyList();
        }

        @GET
        @Path("/strings")
        public List<String> returnStringList() {
            return Collections.singletonList("Not a valid resource");
        }

        @GET
        @Path("/string")
        public String returnString() {
            return "Not a resource either";
        }
    }
}
