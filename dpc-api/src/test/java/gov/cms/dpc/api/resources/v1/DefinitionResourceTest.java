package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Resource definition operations")

class DefinitionResourceTest {

    private static final FhirContext ctx = FhirContext.forDstu3();
    private static final ResourceExtension RESOURCES = buildResources();

    @Test
@DisplayName("Fetch all resource definitions 🥳")

    void testFetchAllResources() {
        final Response response = RESOURCES.target("/v1/StructureDefinition")
                .request(FHIRMediaTypes.FHIR_JSON)
                .get();

        assertEquals(HttpStatus.OK_200, response.getStatus(), "Should have succeeded");

        final Bundle bundle = ctx.newJsonParser().parseResource(Bundle.class, (InputStream) response.getEntity());

        assertEquals(7, bundle.getEntry().size(), "Should have all the structure definitions");
    }

    @Test
@DisplayName("Fetch specific resource definition 🥳")

    void testFetchSpecificResource() {
        // Fetch the patient resource
        final Response response = RESOURCES.target("/v1/StructureDefinition/dpc-profile-patient")
                .request(FHIRMediaTypes.FHIR_JSON)
                .get();

        assertEquals(HttpStatus.OK_200, response.getStatus(), "Should have succeeded");

        final StructureDefinition definition = ctx.newJsonParser().parseResource(StructureDefinition.class, (InputStream) response.getEntity());

        assertEquals(PatientProfile.PROFILE_URI, definition.getUrl(), "Should have matching URLs");
    }

    @Test
@DisplayName("Missing resource definition 🤮")

    void testMissingResource() {
        // Fetch the patient resource
        final Response response = RESOURCES.target("/v1/StructureDefinition/dpc-patient-gone")
                .request(FHIRMediaTypes.FHIR_JSON)
                .get();

        assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus(), "Should not have found the resource");
    }

    private static ResourceExtension buildResources() {

        final DPCProfileSupport profileSupport = new DPCProfileSupport(ctx);

        final DefinitionResource definitionResource = new DefinitionResource(ctx, profileSupport);
        return APITestHelpers.buildResourceExtension(ctx,
                List.of(definitionResource),
                Collections.emptyList(), true);
    }
}
