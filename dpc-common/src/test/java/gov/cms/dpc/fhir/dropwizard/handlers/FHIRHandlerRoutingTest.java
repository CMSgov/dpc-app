package gov.cms.dpc.fhir.dropwizard.handlers;

import gov.cms.dpc.fhir.dropwizard.handlers.r4.FHIRHandler;
import ca.uhn.fhir.context.FhirContext;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.DefaultFHIRExceptionMapper;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class makes sure we can handle writing stu3 and r4 responses.  Once stu3 and v1 go away we should delete it.
 */
// TODO: Delete this class after we get rid of stu3.
@ExtendWith(DropwizardExtensionsSupport.class)
class FHIRHandlerRoutingTest {
    static {
        JerseyGuiceUtils.reset();
    }

    private static final FhirContext STU3 = FhirContext.forDstu3();
    private static final FhirContext R4 = FhirContext.forR4();
    private static final ResourceExtension RESOURCE = ResourceExtension.builder()
            .setRegisterDefaultExceptionMappers(false)
            .addResource(new TestResource())
            .addProvider(new gov.cms.dpc.fhir.dropwizard.handlers.stu3.FHIRHandler(STU3))
            .addProvider(new FHIRHandler(R4))
            .addProvider(DefaultFHIRExceptionMapper.class)
            .build();

    @ParameterizedTest
    @ValueSource(strings = {"v1", "r4"})
    void readsAndWritesTheRequestedVersion(String version) {
        String json = "{\"resourceType\":\"Patient\",\"id\":\"test-patient\",\"name\":[{\"family\":\"García\"}]}";
        try (Response response = RESOURCE.target(version + "/Patient")
                .request(FHIRMediaTypes.FHIR_JSON).post(Entity.entity(json, FHIRMediaTypes.FHIR_JSON))) {
            assertEquals(200, response.getStatus());
            IBaseResource patient = context(version).newJsonParser().parseResource(response.readEntity(String.class));
            assertEquals("test-patient", patient.getIdElement().getIdPart());
            if (patient instanceof org.hl7.fhir.r4.model.Patient r4Patient) {
                assertEquals("García", r4Patient.getNameFirstRep().getFamily());
            } else {
                assertEquals("García", ((org.hl7.fhir.dstu3.model.Patient) patient).getNameFirstRep().getFamily());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1", "r4"})
    void serializesErrorsInTheRequestedVersion(String version) {
        try (Response response = RESOURCE.target(version + "/error").request(FHIRMediaTypes.FHIR_JSON).get()) {
            assertOutcome(response, version, 500);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1", "r4"})
    void returnsBadRequestForMalformedJson(String version) {
        try (Response response = RESOURCE.target(version + "/Patient")
                .request(FHIRMediaTypes.FHIR_JSON).post(Entity.entity("not json", FHIRMediaTypes.FHIR_JSON))) {
            assertOutcome(response, version, 400);
        }
    }

    private FhirContext context(String version) {
        return version.equals("r4") ? R4 : STU3;
    }

    private void assertOutcome(Response response, String version, int status) {
        assertEquals(status, response.getStatus());
        assertEquals(FHIRMediaTypes.FHIR_JSON, response.getMediaType().toString());
        IBaseResource outcome = context(version).newJsonParser().parseResource(response.readEntity(String.class));
        if (version.equals("r4")) {
            assertInstanceOf(org.hl7.fhir.r4.model.OperationOutcome.class, outcome);
        } else {
            assertInstanceOf(org.hl7.fhir.dstu3.model.OperationOutcome.class, outcome);
        }
    }

    @Path("/")
    @FHIR
    @Consumes(FHIRMediaTypes.FHIR_JSON)
    @Produces(FHIRMediaTypes.FHIR_JSON)
    public static class TestResource {
        @POST
        @Path("/v1/Patient")
        public org.hl7.fhir.dstu3.model.Patient echoStu3(org.hl7.fhir.dstu3.model.Patient patient) {
            return patient;
        }

        @POST
        @Path("/r4/Patient")
        public org.hl7.fhir.r4.model.Patient echoR4(org.hl7.fhir.r4.model.Patient patient) {
            return patient;
        }

        @GET
        @Path("/v1/error")
        public Response stu3Error() {
            throw new IllegalStateException("test error");
        }

        @GET
        @Path("/r4/error")
        public Response r4Error() {
            throw new IllegalStateException("test error");
        }
    }
}
