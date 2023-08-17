package gov.cms.dpc.fhir.dropwizard.handlers;

import ca.uhn.fhir.context.FhirContext;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.fhir.annotations.BundleReturnProperties;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericType;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(DropwizardExtensionsSupport.class)
public class BundleHandlerTest {
    static {
        // otherwise our testContainer doesn't get assembled properly
        JerseyGuiceUtils.reset();
    }

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
        assertAll(() -> assertEquals(Bundle.BundleType.SEARCHSET, bundle.getType(), "Should be a search set"),
                () -> assertEquals(1, bundle.getTotal(), "Should have a single entry"),
                () -> assertEquals(0, bundle.getLink().size(), "Should not have any links"),
                () -> assertEquals(Patient.class, bundle.getEntryFirstRep().getResource().getClass(), "Should be a patient"),
                () -> assertEquals("Patient/test-patient", bundle.getEntryFirstRep().getResource().getId(), "Should have the correct id"),
                () -> assertEquals(Enumerations.AdministrativeGender.MALE, ((Patient) bundle.getEntryFirstRep().getResource()).getGender(), "Should have the correct gender"));
    }

    @Test
    void testRawBundle() {
        final String bundleString = resource
                .target("/raw")
                .request()
                .get(String.class);
        final Bundle bundle = ctx.newJsonParser().parseResource(Bundle.class, bundleString);
        assertAll(() -> assertEquals(Bundle.BundleType.BATCH, bundle.getType(), "Should be a batch response"),
                () -> assertEquals(1, bundle.getLink().size(), "Should have a link"),
                () -> assertEquals("http://test.url", bundle.getLinkFirstRep().getUrl(), "Should have correct URL"));
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
    void testCollectionBundle() {
        final String bundleString = resource
                .target("/collection")
                .request()
                .post(null)
                .readEntity(String.class);

        final Bundle bundle = ctx.newJsonParser().parseResource(Bundle.class, bundleString);

        assertAll(() -> assertEquals(Bundle.BundleType.COLLECTION, bundle.getType(), "Should be a collection"),
                () -> assertEquals(0, bundle.getTotal(), "Collections do not have totals"),
                () -> assertEquals(2, bundle.getEntry().size(), "Should have 2 members"));

        // Find the patient and the practitioner resources

        final Patient patient = bundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r.getResourceType().getPath().equals(DPCResourceType.Patient.getPath()))
                .map(r -> (Patient) r)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Could not find patient"));

        assertAll(() -> assertEquals(Enumerations.AdministrativeGender.MALE, patient.getGender(), "Should have gender"),
                () -> assertEquals("Patient/test-patient", patient.getId(), "Should have correct id"));

        final Practitioner practitioner = bundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r.getResourceType().getPath().equals(DPCResourceType.Practitioner.getPath()))
                .map(r -> (Practitioner) r)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Could not find practitioner"));

        assertEquals("Practitioner/test-practitioner", practitioner.getId(), "Should have correct id");
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

        @POST
        @Path("/collection")
        @BundleReturnProperties(bundleType = Bundle.BundleType.COLLECTION)
        public List<Resource> returnCollectionBundle() {
            final Patient p = new Patient();
            p.setId("test-patient");
            p.setGender(Enumerations.AdministrativeGender.MALE);

            final Practitioner practitioner = new Practitioner();
            practitioner.setId("test-practitioner");

            return List.of(p, practitioner);
        }

        @GET
        @Path("/raw")
        public Bundle returnRawBundle() {
            final Group group = new Group();
            group.setId("test-group");

            final Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.BATCH);
            bundle.setLink(Collections.singletonList(new Bundle.BundleLinkComponent().setUrl("http://test.url")));

            bundle.addEntry().setResource(group);

            return bundle;
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
