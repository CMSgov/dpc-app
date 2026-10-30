package gov.cms.dpc.fhir.parameters;

import ca.uhn.fhir.context.FhirContext;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.r4.FHIRParameter;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.DefaultFHIRExceptionMapper;
import gov.cms.dpc.fhir.dropwizard.handlers.r4.FHIRHandler;
import gov.cms.dpc.fhir.parameters.r4.FHIRParamValueFactory;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.Consumes;
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
 * This test makes sure we can use the @FHIRParameter annotation to pull resources from FHIR parameters in both STU3
 * and R4.  Once we're done with STU3 we can probably get rid of it.
 */
// TODO: Remove when we drop v1/stu3
@ExtendWith(DropwizardExtensionsSupport.class)
class FHIRParameterRoutingTest {
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
            .addProvider(new gov.cms.dpc.fhir.parameters.stu3.FHIRParamValueFactory(STU3))
            .addProvider(new FHIRParamValueFactory(R4))
            .addProvider(DefaultFHIRExceptionMapper.class)
            .build();

    @ParameterizedTest
    @ValueSource(strings = {"v1", "r4"})
    void extractsNamedBundle(String version) {
        String json = "{\"resourceType\":\"Parameters\",\"parameter\":["
                + "{\"name\":\"other\",\"resource\":{\"resourceType\":\"Patient\"}},"
                + "{\"name\":\"resource\",\"resource\":{\"resourceType\":\"Bundle\",\"id\":\"selected\",\"type\":\"transaction\"}}]}";
        assertBundle(version, "named", json);
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1", "r4"})
    void extractsFirstBundle(String version) {
        String json = "{\"resourceType\":\"Parameters\",\"parameter\":["
                + "{\"name\":\"first\",\"resource\":{\"resourceType\":\"Bundle\",\"id\":\"selected\",\"type\":\"transaction\"}}]}";
        assertBundle(version, "first", json);
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1", "r4"})
    void rejectsMissingParameter(String version) {
        assertBadRequest(version, "{\"resourceType\":\"Parameters\"}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1", "r4"})
    void rejectsWrongResourceType(String version) {
        assertBadRequest(version, "{\"resourceType\":\"Parameters\",\"parameter\":["
                + "{\"name\":\"resource\",\"resource\":{\"resourceType\":\"Patient\"}}]}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1", "r4"})
    void rejectsMalformedJson(String version) {
        assertBadRequest(version, "not json");
    }

    private void assertBundle(String version, String operation, String json) {
        try (Response response = post(version, operation, json)) {
            assertEquals(200, response.getStatus());
            IBaseResource bundle = context(version).newJsonParser().parseResource(response.readEntity(String.class));
            assertEquals("selected", bundle.getIdElement().getIdPart());
            if (version.equals("r4")) {
                assertInstanceOf(org.hl7.fhir.r4.model.Bundle.class, bundle);
            } else {
                assertInstanceOf(org.hl7.fhir.dstu3.model.Bundle.class, bundle);
            }
        }
    }

    private void assertBadRequest(String version, String json) {
        try (Response response = post(version, "named", json)) {
            assertEquals(400, response.getStatus());
            IBaseResource outcome = context(version).newJsonParser().parseResource(response.readEntity(String.class));
            if (version.equals("r4")) {
                assertInstanceOf(org.hl7.fhir.r4.model.OperationOutcome.class, outcome);
            } else {
                assertInstanceOf(org.hl7.fhir.dstu3.model.OperationOutcome.class, outcome);
            }
        }
    }

    private Response post(String version, String operation, String json) {
        return RESOURCE.target(version + "/" + operation).request(FHIRMediaTypes.FHIR_JSON)
                .post(Entity.entity(json, FHIRMediaTypes.FHIR_JSON));
    }

    private FhirContext context(String version) {
        return version.equals("r4") ? R4 : STU3;
    }

    @Path("/")
    @FHIR
    @Consumes(FHIRMediaTypes.FHIR_JSON)
    @Produces(FHIRMediaTypes.FHIR_JSON)
    public static class TestResource {
        @POST
        @Path("/v1/named")
        public org.hl7.fhir.dstu3.model.Bundle stu3Named(
                @gov.cms.dpc.fhir.annotations.stu3.FHIRParameter(name = "resource") org.hl7.fhir.dstu3.model.Bundle bundle) {
            return bundle;
        }

        @POST
        @Path("/r4/named")
        public org.hl7.fhir.r4.model.Bundle r4Named(@FHIRParameter(name = "resource") org.hl7.fhir.r4.model.Bundle bundle) {
            return bundle;
        }

        @POST
        @Path("/v1/first")
        public org.hl7.fhir.dstu3.model.Bundle stu3First(
                @gov.cms.dpc.fhir.annotations.stu3.FHIRParameter org.hl7.fhir.dstu3.model.Bundle bundle) {
            return bundle;
        }

        @POST
        @Path("/r4/first")
        public org.hl7.fhir.r4.model.Bundle r4First(@FHIRParameter org.hl7.fhir.r4.model.Bundle bundle) {
            return bundle;
        }
    }
}
