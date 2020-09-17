package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.exceptions.JsonParseExceptionMapper;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.dropwizard.handlers.BundleHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.DefaultFHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.HAPIExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.JerseyExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.PersistenceExceptionHandler;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.ProfileValidator;
import gov.cms.dpc.fhir.validations.dropwizard.FHIRValidatorProvider;
import gov.cms.dpc.fhir.validations.dropwizard.InjectingConstraintValidatorFactory;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.ValidationSupportChain;
import org.hl7.fhir.dstu3.model.*;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class APITestHelpers {
    private static final String ATTRIBUTION_URL = "http://localhost:3500/v1";
    public static final String ORGANIZATION_ID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0";
    private static final String ATTRIBUTION_TRUNCATE_TASK = "http://localhost:9902/tasks/truncate";
    public static String BASE_URL = "https://dpc.cms.gov/api";
    public static String ORGANIZATION_NPI = "1111111112";

    private static final ObjectMapper mapper = new ObjectMapper();

    private APITestHelpers() {
        // Not used
    }

    public static OrganizationPrincipal makeOrganizationPrincipal() {
        Organization org = new Organization();
        org.setId(ORGANIZATION_ID);
        return new OrganizationPrincipal(org);
    }

    public static OrganizationPrincipal makeOrganizationPrincipal(String id) {
        Organization org = new Organization();
        org.setId(id);
        return new OrganizationPrincipal(org);
    }

    public static IGenericClient buildAttributionClient(FhirContext ctx) {
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        IGenericClient client = ctx.newRestfulGenericClient(ATTRIBUTION_URL);

        // Disable logging for tests
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(false);
        loggingInterceptor.setLogResponseSummary(false);
        client.registerInterceptor(loggingInterceptor);

        return client;
    }

    public static void setupPractitionerTest(IGenericClient client, IParser parser) throws IOException {
        try (InputStream inputStream = APITestHelpers.class.getClassLoader().getResourceAsStream("provider_bundle.json")) {
            final Parameters providerParameters = (Parameters) parser.parseResource(inputStream);

            client
                    .operation()
                    .onType(Practitioner.class)
                    .named("submit")
                    .withParameters(providerParameters)
                    .encodedJson()
                    .execute();
        }
    }

    public static void setupPatientTest(IGenericClient client, IParser parser) throws IOException {
        try (InputStream inputStream = APITestHelpers.class.getClassLoader().getResourceAsStream("patient_bundle.json")) {
            final Parameters patientParameters = (Parameters) parser.parseResource(inputStream);

            client
                    .operation()
                    .onType(Patient.class)
                    .named("submit")
                    .withParameters(patientParameters)
                    .encodedJson()
                    .execute();
        }
    }

    /**
     * Build Dropwizard test instance with a specific subset of Resources and Providers
     *
     * @param ctx        - {@link FhirContext} context to use
     * @param resources  - {@link List} of resources to add to test instance
     * @param providers  - {@link List} of providers to add to test instance
     * @param validation - {@code true} enable custom validation. {@code false} Disable custom validation
     * @return - {@link ResourceExtension}
     */
    public static ResourceExtension buildResourceExtension(FhirContext
                                                                   ctx, List<Object> resources, List<Object> providers, boolean validation) {

        final FHIRHandler fhirHandler = new FHIRHandler(ctx);
        final var builder = ResourceExtension
                .builder()
                .setRegisterDefaultExceptionMappers(false)
                .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addProvider(fhirHandler)
                .addProvider(new BundleHandler(fhirHandler))
                .addProvider(JerseyExceptionHandler.class)
                .addProvider(PersistenceExceptionHandler.class)
                .addProvider(HAPIExceptionHandler.class)
                .addProvider(DefaultFHIRExceptionHandler.class)
                .addProvider(JsonParseExceptionMapper.class)
                .addProvider(new AuthValueFactoryProvider.Binder<>(OrganizationPrincipal.class));

        // Optionally enable validation
        if (validation) {
            // Validation config
            final DPCFHIRConfiguration.FHIRValidationConfiguration config = new DPCFHIRConfiguration.FHIRValidationConfiguration();
            config.setEnabled(true);
            config.setSchematronValidation(true);
            config.setSchemaValidation(true);

            final DPCProfileSupport dpcModule = new DPCProfileSupport(ctx);
            final ValidationSupportChain support = new ValidationSupportChain(new DefaultProfileValidationSupport(), dpcModule);
            final InjectingConstraintValidatorFactory constraintFactory = new InjectingConstraintValidatorFactory(
                    Set.of(new ProfileValidator(new FHIRValidatorProvider(ctx, config, support).get())));

            builder.setValidator(provideValidator(constraintFactory));
        }

        resources.forEach(builder::addResource);
        providers.forEach(builder::addProvider);

        return builder.build();
    }

    static <C extends io.dropwizard.Configuration> void setupApplication(DropwizardTestSupport<C> application) throws
            Exception {
        ConfigFactory.invalidateCaches();
        // Truncate attribution database
        truncateDatabase();
        application.before();
        // Truncate the Auth DB
        application.getApplication().run("db", "drop-all", "--confirm-delete-everything", "ci.application.conf");
        application.getApplication().run("db", "migrate", "ci.application.conf");

    }

    private static void truncateDatabase() throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(ATTRIBUTION_TRUNCATE_TASK);

            try (CloseableHttpResponse execute = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, execute.getStatusLine().getStatusCode(), "Should have truncated database");
            }
        }
    }

    static <C extends io.dropwizard.Configuration> void checkHealth(DropwizardTestSupport<C> application) throws
            IOException {
        // URI of the API Service Healthcheck
        final String healthURI = String.format("http://localhost:%s/healthcheck", application.getAdminPort());
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet healthCheck = new HttpGet(healthURI);

            try (CloseableHttpResponse execute = client.execute(healthCheck)) {
                assertEquals(HttpStatus.OK_200, execute.getStatusLine().getStatusCode(), "Should be healthy");
            }
        }
    }

    private static Validator provideValidator(InjectingConstraintValidatorFactory factory) {
        return Validation.byDefaultProvider()
                .configure().constraintValidatorFactory(factory)
                .buildValidatorFactory().getValidator();
    }

    public static Practitioner createPractitionerResource(String npi, String orgID) {
        final Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier().setValue(npi).setSystem(DPCIdentifierSystem.NPPES.getSystem());
        practitioner.addName()
                .setFamily("Practitioner").addGiven("Test");

        // Meta data which includes the Org we're using
        final Meta meta = new Meta();
        meta.addTag(DPCIdentifierSystem.DPC.getSystem(), orgID, "OrganizationID");
        practitioner.setMeta(meta);

        return practitioner;
    }

    public static Patient createPatientResource(String mbi, String organizationID) {
        final Patient patient = new Patient();
        patient.addIdentifier()
                .setSystem(DPCIdentifierSystem.MBI.getSystem())
                .setValue(mbi);

        patient.addName().setFamily("Patient").addGiven("Test");
        patient.setBirthDate(Date.valueOf("1990-01-01"));
        patient.setGender(Enumerations.AdministrativeGender.OTHER);
        patient.setManagingOrganization(new Reference(new IdType("Organization", organizationID)));

        return patient;
    }

}
