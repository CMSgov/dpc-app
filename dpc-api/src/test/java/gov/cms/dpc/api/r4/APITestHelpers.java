package gov.cms.dpc.api.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.exceptions.JsonParseExceptionMapper;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.DataFormatExceptionMapper;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.DefaultFHIRExceptionMapper;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.HAPIExceptionMapper;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.JerseyExceptionMapper;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.PersistenceExceptionMapper;
import gov.cms.dpc.fhir.dropwizard.handlers.r4.BundleHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.r4.FHIRHandler;
import gov.cms.dpc.fhir.validations.dropwizard.InjectingConstraintValidatorFactory;
import gov.cms.dpc.fhir.validations.r4.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.r4.ProfileValidator;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.factories.r4.FHIRPatientBuilder;
import gov.cms.dpc.testing.factories.r4.FHIRPractitionerBuilder;
import gov.cms.dpc.testing.utils.MBIUtil;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.validation.Validation;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import ru.vyarus.dropwizard.guice.module.context.SharedConfigurationState;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class APITestHelpers {
    private static final String ATTRIBUTION_URL = "http://localhost:3500/r4";
    private static final String ATTRIBUTION_TRUNCATE_TASK = "http://localhost:9902/tasks/truncate";
    private static final String CONSENT_TRUNCATE_TASK = "http://localhost:9901/tasks/truncate";
    private static final String CONFIG_PATH = "src/test/resources/test.application.yml";
    public static final String ORGANIZATION_ID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0";
    public static String ORGANIZATION_NPI = "1111111112";

    private APITestHelpers() {
        // Not used
    }

    public static OrganizationPrincipal makeOrganizationPrincipal() {
        return makeOrganizationPrincipal(ORGANIZATION_ID);
    }

    public static OrganizationPrincipal makeOrganizationPrincipal(String id) {
        // Authentication principals remain STU3 even when the API resource is R4.
        Organization organization = new Organization();
        organization.setId(id);
        organization.addIdentifier(new Identifier()
                .setSystem(DPCIdentifierSystem.NPPES.getSystem())
                .setValue(ORGANIZATION_NPI));
        return new OrganizationPrincipal(organization);
    }

    public static IGenericClient buildAttributionClient(FhirContext context) {
        context.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        IGenericClient client = context.newRestfulGenericClient(ATTRIBUTION_URL);

        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(false);
        loggingInterceptor.setLogResponseSummary(false);
        client.registerInterceptor(loggingInterceptor);
        client.registerInterceptor(new APIAuthHelpers.RepeatableResponseInterceptor());

        return client;
    }

    public static ResourceExtension buildResourceExtension(FhirContext context, List<Object> resources,
                                                            List<Object> providers, boolean validation) {
        FHIRHandler fhirHandler = new FHIRHandler(context);
        var builder = ResourceExtension.builder()
                .setRegisterDefaultExceptionMappers(false)
                .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addProvider(fhirHandler)
                .addProvider(new BundleHandler(fhirHandler))
                .addProvider(JerseyExceptionMapper.class)
                .addProvider(PersistenceExceptionMapper.class)
                .addProvider(HAPIExceptionMapper.class)
                .addProvider(DefaultFHIRExceptionMapper.class)
                .addProvider(DataFormatExceptionMapper.class)
                .addProvider(JsonParseExceptionMapper.class)
                .addProvider(new AuthValueFactoryProvider.Binder<>(OrganizationPrincipal.class));

        if (validation) {
            ValidationSupportChain support = new ValidationSupportChain(
                    new DPCProfileSupport(context),
                    new DefaultProfileValidationSupport(context),
                    new InMemoryTerminologyServerValidationSupport(context));
            FhirValidator validator = context.newValidator();
            validator.registerValidatorModule(new FhirInstanceValidator(support));
            var factory = new InjectingConstraintValidatorFactory(Set.of(new ProfileValidator(validator)));
            builder.setValidator(Validation.byDefaultProvider()
                    .configure().constraintValidatorFactory(factory)
                    .buildValidatorFactory().getValidator());
        }

        resources.forEach(builder::addResource);
        providers.forEach(builder::addProvider);
        return builder.build();
    }

    public static <C extends io.dropwizard.core.Configuration> void setupApplication(DropwizardTestSupport<C> application)
            throws Exception {
        truncateDatabase();
        application.before();
        SharedConfigurationState.clear();
        application.getApplication().run("db", "drop-all", "--confirm-delete-everything", CONFIG_PATH);
        SharedConfigurationState.clear();
        application.getApplication().run("db", "migrate", CONFIG_PATH);
    }

    private static void truncateDatabase() throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            for (String url : List.of(ATTRIBUTION_TRUNCATE_TASK, CONSENT_TRUNCATE_TASK)) {
                try (CloseableHttpResponse response = client.execute(new HttpPost(url))) {
                    assertEquals(HttpStatus.OK_200, response.getCode(), "Should have truncated DB at " + url);
                }
            }
        }
    }

    public static <C extends io.dropwizard.core.Configuration> void checkHealth(DropwizardTestSupport<C> application)
            throws IOException {
        String healthURI = String.format("http://localhost:%s/healthcheck", application.getAdminPort());
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(new HttpGet(healthURI))) {
                assertEquals(HttpStatus.OK_200, response.getCode(), "Should be healthy");
            }
        }
    }

    public static Practitioner createPractitionerResource(String npi, String orgID) {
        return FHIRPractitionerBuilder.newBuilder()
                .withNpi(npi)
                .withOrgTag(orgID)
                .withName("Test", "Practitioner")
                .withId(UUID.randomUUID())
                .build();
    }

    public static void setupPractitionerTest(IGenericClient client, IParser parser) throws IOException {
        try (InputStream inputStream = APITestHelpers.class.getClassLoader().getResourceAsStream("r4/provider_bundle.json")) {
            if (inputStream == null) {
                throw new IOException("Missing r4/provider_bundle.json test fixture");
            }
            final Parameters providerParameters = (Parameters) parser.parseResource(inputStream);
            client.operation()
                    .onType(Practitioner.class)
                    .named("submit")
                    .withParameters(providerParameters)
                    .encodedJson()
                    .execute();
        }
    }

    public static MethodOutcome createResource(IGenericClient client, IBaseResource resource) {
        return client.create().resource(resource).encodedJson().execute();
    }

    public static <T extends IBaseResource> T getResourceById(IGenericClient client, Class<T> clazz, String resourceId) {
        return client.read().resource(clazz).withId(resourceId).encodedJson().execute();
    }

    public static Bundle resourceSearch(IGenericClient client, DPCResourceType resourceType) {
        return client.search().forResource(resourceType.name())
                .whereMap(Map.of()).returnBundle(Bundle.class).encodedJson().execute();
    }

    public static void deleteResourceById(IGenericClient client, DPCResourceType resourceType, String resourceId) {
        client.delete().resourceById(resourceType.name(), resourceId).execute();
    }

    public static void updateResource(IGenericClient client, String id, IBaseResource resource) {
        IUpdateExecutable executable = client.update().resource(resource).withId(id).encodedJson();
        executable.execute();
    }

    public static Patient createPatientResource(String organizationID) {
        return createPatientResource(MBIUtil.generateMBI(), organizationID);
    }

    public static Patient createPatientResource(String mbi, String organizationID) {
        return FHIRPatientBuilder.newBuild()
                .withMbi(mbi)
                .withBirthDate("1990-01-01")
                .withName("Test", "Patient")
                .withGender(Enumerations.AdministrativeGender.OTHER)
                .managedBy(organizationID)
                .withId(UUID.randomUUID())
                .build();
    }

    public static List<Patient> createPatientResources(String organizationID, int numPatients) {
        List<Patient> patients = new ArrayList<>(numPatients);

        for (int i = 0; i < numPatients; i++) {
            patients.add(createPatientResource(MBIUtil.generateMBI(), organizationID));
        }
        return patients;
    }

    public static Provenance createProvenance(String orgId, String practitionerId, List<String> patientIds) {
        if (patientIds.isEmpty()) {
            throw new IllegalArgumentException("R4 Provenance requires at least one patient target");
        }

        final Coding reasonCoding = new Coding().setSystem("http://hl7.org/fhir/v3/ActReason").setCode("TREAT");

        final Coding roleCode = new Coding()
                .setSystem("http://hl7.org/fhir/v3/RoleClass")
                .setCode("AGNT");

        final CodeableConcept roleConcept = new CodeableConcept().addCoding(roleCode);
        final Provenance.ProvenanceAgentComponent component = new Provenance.ProvenanceAgentComponent()
                .setRole(Collections.singletonList(roleConcept))
                .setWho(new Reference(new IdType("Organization", new IdType(orgId).getIdPart())))
                .setOnBehalfOf(new Reference(new IdType("Practitioner", new IdType(practitionerId).getIdPart())));

        final Provenance provenance = new Provenance()
                .setRecorded(Date.valueOf(Instant.now().atZone(ZoneOffset.UTC).toLocalDate()))
                .setReason(Collections.singletonList(new CodeableConcept().addCoding(reasonCoding)))
                .addAgent(component);

        for (String patientId : patientIds) {
            provenance.addTarget(new Reference(new IdType("Patient", new IdType(patientId).getIdPart())));
        }
        return provenance;
    }
}
