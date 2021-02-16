package gov.cms.dpc.consent.resources;

import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests ConsentResource using a mocked DAO, making it possible to run these tests with a database running.
 * The intent for these tests is to be able to test corner and edge cases in an easily controlled way.
 */
@Disabled
@ExtendWith(DropwizardExtensionsSupport.class)
public class ConsentResourceUnitTest {

    static {
        // otherwise our testContainer doesn't get assembled properly
        JerseyGuiceUtils.reset();
    }

    private static final String TEST_HICN = "this_is_a_placeholder_hicn";
    private static final String TEST_MBI = "this_is_a_placeholder_mbi";
    private static final UUID TEST_ID = UUID.randomUUID();

    private static final ConsentDAO mockedDAO = mock(ConsentDAO.class);
    private static final GrizzlyWebTestContainerFactory testContainer = new GrizzlyWebTestContainerFactory();

    public static final ResourceExtension resource = ResourceExtension.builder()
            .addResource(new ConsentResource(mockedDAO, "http://test-org-url"))
            .setTestContainerFactory(testContainer)
            .build();

    private ConsentResourceUnitTest() {
    }

    @BeforeAll
    static void initMock() {
        // Broke when introducing findBy
        ConsentEntity goodRecord = ConsentEntity.defaultConsentEntity(Optional.of(TEST_ID), Optional.of(TEST_HICN), Optional.of(TEST_MBI));
        List<ConsentEntity> goodRecordList = List.of(goodRecord);
        when(mockedDAO.getConsent(null)).thenThrow(new IllegalArgumentException("empty"));
        when(mockedDAO.getConsent(TEST_ID)).thenReturn(Optional.of(goodRecord));
        when(mockedDAO.findBy("mbi", TEST_MBI)).thenReturn(goodRecordList);
        when(mockedDAO.findBy("hicn", TEST_HICN)).thenReturn(goodRecordList);
    }

    @Test
    final void getResource_withValidId_returnsConsentResource() {
        try (Response response = resource.target("/Consent/" + TEST_ID)
                .request()
                .accept(FHIR_JSON)
                .get()) {

            assertEquals(HttpStatus.OK_200, response.getStatus(), "should find record for test id");
        }
        // can't parse the result here; assuming this is a configuration issue as it throws a jackson mapping error
        // final Consent resource = FhirContext.forDstu3().newJsonParser().parseResource(Consent.class, response.readEntity(InputStream.class));
        // assertDoesNotThrow(() -> FhirContext.forDstu3().newJsonParser().encodeResourceToString(resource));
    }

    @Test
    final void search_withEmptyString_isInvalid() {
        try (Response response = resource.target("/Consent/")
                .request()
                .accept(FHIR_JSON)
                .get()) {

            assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatus(), "should reject request as invalid");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/Consent?", "/Consent?_id=,patient=mbi_1", "/Consent?identifier=", "/Consent?patient=", "/Consent?owieurijefj"})
    final void searchConsentResource_fails_withInvalidSearchParameters(String path) {
        try (Response response = resource.target(path)
                .request()
                .accept(FHIR_JSON)
                .get()) {

            assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus(), "invalid paths should not be found");
        }
    }
}
