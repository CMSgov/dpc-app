package gov.cms.dpc.consent.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.consent.AbstractConsentTest;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.converters.entities.ConsentEntityConverter;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests ConsentResource using a live database. These tests will fail if the database is not running or
 * seed data is not loaded.
 */
class ConsentResourceTest extends AbstractConsentTest {

    private static final String TEST_CONSENT_UUID = "3a51c388-a9b0-42e2-afe4-688a2b6cef95";
    private static final String TEST_CONSENT_REF = String.format("Consent/%s", TEST_CONSENT_UUID);

    private ConsentResourceTest() {
    }

    @Test
    final void createConsent() {
        final IGenericClient client = createFHIRClient(ctx, getServerURL());
        Consent consent = new Consent();

        consent.setStatus(Consent.ConsentState.ACTIVE);

        Coding categoryCoding = new Coding("http://loinc.org","64292-6", null);
        CodeableConcept category = new CodeableConcept();
        category.setCoding(List.of(categoryCoding));
        consent.setCategory(List.of(category));

        String patientRefPath = "/Patient?identity=|0OO0OO0OO00";
        consent.setPatient(new Reference("http://api.url" + patientRefPath));

        Date date = Date.from(Instant.now());
        consent.setDateTime(date);

        String policyUrl = "http://hl7.org/fhir/ConsentPolicy/opt-out";
        consent.setPolicyRule(policyUrl);

        MethodOutcome outcome = client
                .create()
                .resource(consent)
                .encodedJson()
                .execute();

        assertTrue(outcome.getCreated());

        Consent result = (Consent) outcome.getResource();
        assertTrue(result.getPatient().getReference().endsWith(patientRefPath));
        assertEquals(policyUrl, result.getPolicyRule());
        assertEquals(Date.from(date.toInstant().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC)), result.getDateTime());
    }

    @Test
    final void createConsent_fails_withInvalidMbi() {
        final IGenericClient client = createFHIRClient(ctx, getServerURL());
        Consent consent = new Consent();

        consent.setStatus(Consent.ConsentState.ACTIVE);

        Coding categoryCoding = new Coding();
        CodeableConcept category = new CodeableConcept();
        category.setCoding(List.of(categoryCoding));
        consent.setCategory(List.of(category));

        String patientRefPath = "/Patient?identity=|ABCDEFG";
        consent.setPatient(new Reference("http://api.url" + patientRefPath));

        Date date = Date.from(Instant.now());
        consent.setDateTime(date);

        String policyUrl = "http://hl7.org/fhir/ConsentPolicy/opt-out";
        consent.setPolicyRule(policyUrl);

        ICreateTyped createOp = client
                .create()
                .resource(consent);

        assertThrows(UnprocessableEntityException.class, createOp::execute);
    }

    @Test
    final void getConsentResource_fails_withNonExtantId() {
        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final IReadExecutable<Consent> sut = client
                .read()
                .resource(Consent.class)
                .withId("1")
                .encodedJson();

        assertThrows(ResourceNotFoundException.class, sut::execute, "should not find resource");
    }

    @Test
    final void getConsentResource_finds_validId() {
        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final Consent sut = client
                .read()
                .resource(Consent.class)
                .withId(TEST_CONSENT_UUID)
                .encodedJson()
                .execute();

        assertEquals(ConsentEntityConverter.OPT_IN_MAGIC, sut.getPolicyRule());
        assertEquals(TEST_CONSENT_REF, sut.getId());
    }

    @Test
    final void searchConsentResource_fails_withNoParams() {
        // an attempt to the resource with an empty value is routed to the search endpoint

        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        @SuppressWarnings("rawtypes") final IQuery sut = client
                .search()
                .forResource(Consent.class)
                .encodedJson();

        assertThrows(InvalidRequestException.class, sut::execute, "should fail with no search params");
    }

    @Test
    final void searchConsentResource_finds_validIdParam() {

        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final Bundle sut = client
                .search()
                .forResource(Consent.class)
                .where(Consent.RES_ID.exactly().identifier(TEST_CONSENT_UUID))
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();

        final Consent found = (Consent) sut.getEntryFirstRep().getResource();

        assertEquals(ConsentEntityConverter.OPT_IN_MAGIC, found.getPolicyRule());
        assertEquals(TEST_CONSENT_REF, found.getId());
    }

    @Test
    final void searchConsentResource_finds_validIdentifierParam() {

        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final Bundle sut = client
                .search()
                .forResource(Consent.class)
                .where(Consent.IDENTIFIER.exactly().identifier(TEST_CONSENT_UUID))
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();

        final Consent found = (Consent) sut.getEntryFirstRep().getResource();

        assertEquals(ConsentEntityConverter.OPT_IN_MAGIC, found.getPolicyRule());
        assertEquals(TEST_CONSENT_REF, found.getId());
    }

    @ParameterizedTest
    @CsvSource({"MBI,mbi_1", "HICN,hicn_1"})
    final void searchConsentResource_finds_validPatientParam(String system, String patientId) {

        final IGenericClient client = createFHIRClient(ctx, getServerURL());
        final String patientValue = String.format("%s|%s", DPCIdentifierSystem.valueOf(system).getSystem(), patientId);

        final Bundle sut = client
                .search()
                .forResource(Consent.class)
                .where(new StringClientParam("patient").matches().value(patientValue))
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();

        final Consent found = (Consent) sut.getEntryFirstRep().getResource();
        System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(found));

        assertEquals(ConsentEntityConverter.OPT_IN_MAGIC, found.getPolicyRule());
        assertEquals(TEST_CONSENT_REF, found.getId());
    }

    @ParameterizedTest
    @CsvSource({"MBI,mbi_1", "HICN,hicn_1"})
    final void searchConsentResource_list_of_ids(String system, String patientId) {
        final IGenericClient client = createFHIRClient(ctx, getServerURL());
        final String patientValue = String.format("%s|%s", DPCIdentifierSystem.valueOf(system).getSystem(), patientId);

        final Bundle sut = client
                .search()
                .forResource(Consent.class)
                .where(new StringClientParam("patient").matches().value(patientValue + ","))
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();

        final Consent found = (Consent) sut.getEntryFirstRep().getResource();
        System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(found));

        assertEquals(ConsentEntityConverter.OPT_IN_MAGIC, found.getPolicyRule());
        assertEquals(TEST_CONSENT_REF, found.getId());
    }

    @Test
    final void searchConsentResource_multiple_ids() {
        String patientIds = "mbi_1,hicn_1";
        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final Bundle sut = client
                .search()
                .forResource(Consent.class)
                .where(new StringClientParam("patient").matches().value(patientIds))
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();

        final Consent found = (Consent) sut.getEntryFirstRep().getResource();
        System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(found));

        assertEquals(ConsentEntityConverter.OPT_IN_MAGIC, found.getPolicyRule());
        assertEquals(TEST_CONSENT_REF, found.getId());

    }

    @Test
    final void updateConsent() {
        final IGenericClient client = createFHIRClient(ctx, getServerURL());
        Consent consent = new Consent();

        consent.setStatus(Consent.ConsentState.ACTIVE);

        Coding categoryCoding = new Coding("http://loinc.org","64292-6", null);
        CodeableConcept category = new CodeableConcept();
        category.setCoding(List.of(categoryCoding));
        consent.setCategory(List.of(category));

        String patientRefPath = "/Patient?identity=|0OO0OO0OO00";
        consent.setPatient(new Reference("http://api.url" + patientRefPath));

        Date date = Date.from(Instant.now());
        consent.setDateTime(date);

        String policyUrl = "http://hl7.org/fhir/ConsentPolicy/opt-in";
        consent.setPolicyRule(policyUrl);

        MethodOutcome outcome = client
                .create()
                .resource(consent)
                .encodedJson()
                .execute();

        consent = (Consent) outcome.getResource();
        consent.setPolicyRule("http://hl7.org/fhir/ConsentPolicy/opt-out");

        outcome = client
                .update()
                .resource(consent)
                .withId(consent.getId())
                .execute();

        Consent updatedConsent = (Consent) outcome.getResource();
        assertEquals("http://hl7.org/fhir/ConsentPolicy/opt-out", updatedConsent.getPolicyRule());
    }
}
