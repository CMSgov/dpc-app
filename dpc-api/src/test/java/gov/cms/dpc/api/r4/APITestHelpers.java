package gov.cms.dpc.api.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.exceptions.JsonParseExceptionMapper;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
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
import ru.vyarus.dropwizard.guice.module.context.SharedConfigurationState;

import java.io.IOException;
import java.util.List;
import java.util.Set;

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
        // Authentication principals remain STU3 even when the API resource is R4.
        Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
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
}
