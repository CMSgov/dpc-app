package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.exceptions.JsonParseExceptionMapper;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.dropwizard.handlers.BundleHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.DefaultFHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.HAPIExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.JerseyExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.PersistenceExceptionHandler;
import gov.cms.dpc.fhir.hapi.ContextUtils;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.ProfileValidator;
import gov.cms.dpc.fhir.validations.dropwizard.FHIRValidatorProvider;
import gov.cms.dpc.fhir.validations.dropwizard.InjectingConstraintValidatorFactory;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.testing.factories.FHIRPatientBuilder;
import gov.cms.dpc.testing.factories.FHIRPractitionerBuilder;
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
import org.hl7.fhir.dstu3.model.codesystems.V3RoleClass;
import org.hl7.fhir.instance.model.api.IBaseResource;
import ru.vyarus.dropwizard.guice.module.context.SharedConfigurationState;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class APITestHelpers {
    private static final String ATTRIBUTION_URL = "http://localhost:3500/v1";
    private static final String CONSENT_URL = "http://localhost:3600/v1";
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

    public static IGenericClient buildConsentClient(FhirContext ctx){
        ContextUtils.prefetchResourceModels(ctx, JobQueueBatch.validResourceTypes);
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        return ctx.newRestfulGenericClient(CONSENT_URL);
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
        // dropwizard-guicey will raise a SharedStateError unless we clear the configuration state before each run
        SharedConfigurationState.clear();
        application.getApplication().run("db", "drop-all", "--confirm-delete-everything", "ci.application.conf");
        SharedConfigurationState.clear();
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
        return FHIRPractitionerBuilder.newBuilder()
                .withNpi(npi)
                .withOrgTag(orgID)
                .withName("Test", "Practitioner")
                .build();
    }

    public static Patient createPatientResource(String mbi, String organizationID) {
        return FHIRPatientBuilder.newBuild()
                .withMbi(mbi)
                .withBirthDate("1990-01-01")
                .withName("Test", "Patient")
                .withGender(Enumerations.AdministrativeGender.OTHER)
                .managedBy(organizationID)
                .build();
    }

    public static Provenance createProvenance(String orgId, String practitionerId, List<String> patientIds){
        final Coding reasonCoding = new Coding().setSystem("http://hl7.org/fhir/v3/ActReason").setCode("TREAT");

        final Coding roleCode = new Coding()
                .setSystem(V3RoleClass.AGNT.getSystem())
                .setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept().addCoding(roleCode);
        final Provenance.ProvenanceAgentComponent component = new Provenance.ProvenanceAgentComponent()
                .setRole(Collections.singletonList(roleConcept))
                .setWho(new Reference(new IdType("Organization", orgId)))
                .setOnBehalfOf(new Reference(practitionerId));

        final Provenance provenance = new Provenance()
                .setRecorded(Date.valueOf(Instant.now().atZone(ZoneOffset.UTC).toLocalDate()))
                .setReason(Collections.singletonList(reasonCoding))
                .addAgent(component);

        for(String patientId:patientIds){
            provenance.addTarget(new Reference(patientId));
        }
        return provenance;
    }

    public static MethodOutcome createResource(IGenericClient client, IBaseResource resource, Map<String,String> extraHeaders){
        ICreateTyped iCreateTyped = client.create()
                .resource(resource)
                .encodedJson();

        extraHeaders.forEach(iCreateTyped::withAdditionalHeader);
        return iCreateTyped.execute();
    }

    public static MethodOutcome createResource(IGenericClient client, IBaseResource resource){
        return createResource(client,resource, Maps.newHashMap());
    }

    public  static <T extends IBaseResource> T getResourceById(IGenericClient client, Class<T> clazz, String resourceId){
       return client.read()
                .resource(clazz)
                .withId(resourceId).encodedJson().execute();
    }

    public  static Bundle resourceSearch(IGenericClient client, DPCResourceType resourceType, Map<String,List<String>> searchParams){
        return client
                .search()
                .forResource(resourceType.name())
                .whereMap(searchParams)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
    }

    public  static Bundle resourceSearch(IGenericClient client, DPCResourceType resourceType){
        return resourceSearch(client,resourceType, Maps.newHashMap());
    }

    public static void deleteResourceById(IGenericClient client, DPCResourceType resourceType, String resourceId){
        client.delete()
                .resourceById(resourceType.name(), resourceId)
                .execute();
    }

    public static void updateResource(IGenericClient client, String id, IBaseResource resource, Map<String,String> extraHeaders){
        IUpdateExecutable executable = client
                .update()
                .resource(resource)
                .withId(id)
                .encodedJson();

        extraHeaders.forEach(executable::withAdditionalHeader);
        executable.execute();
    }

    public static void updateResource(IGenericClient client, String id, IBaseResource resource){
        updateResource(client, id, resource, Maps.newHashMap());
    }

    public static Bundle getPatientEverything(IGenericClient client, String patientId, String provenance){
        return client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", provenance)
                .execute();
    }

    public static Bundle doGroupExport(IGenericClient client,String groupId, String provenance){
        return client
                .operation()
                .onInstance(new IdType("Group", groupId))
                .named("$export")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", provenance)
                .execute();
    }
}
