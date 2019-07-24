package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.*;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRValidationExceptionHandler;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.ProfileValidator;
import gov.cms.dpc.fhir.validations.dropwizard.FHIRValidatorProvider;
import gov.cms.dpc.fhir.validations.dropwizard.InjectingConstraintValidatorFactory;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.hl7.fhir.dstu3.model.*;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class APITestHelpers {
    public static final String ATTRIBUTION_URL = "http://localhost:3500/v1";
    public static final String ORGANIZATION_ID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0";
    public static final String ATTRIBUTION_TRUNCATE_TASK = "http://localhost:9902/tasks/truncate";
    public static String BASE_URL = "https://dpc.cms.gov/fhir";

    private APITestHelpers() {
        // Not used
    }

    public static OrganizationPrincipal makeOrganizationPrincipal() {
        Organization org = new Organization();
        org.setId(ORGANIZATION_ID);
        return new OrganizationPrincipal(org);
    }

    public static IGenericClient buildAttributionClient(FhirContext ctx) {
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        return ctx.newRestfulGenericClient(ATTRIBUTION_URL);
    }

    public static void setupPractitionerTest(IGenericClient client, IParser parser) throws IOException {
        try (InputStream inputStream = APITestHelpers.class.getClassLoader().getResourceAsStream("provider_bundle.json")) {
            final Bundle orgBundle = (Bundle) parser.parseResource(inputStream);

            // Post them all
            orgBundle
                    .getEntry()
                    .stream()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .map(resource -> (Practitioner) resource)
                    .forEach(practitioner -> client
                            .create()
                            .resource(practitioner)
                            .encodedJson()
                            .execute());
        }
    }

    public static void setupPatientTest(IGenericClient client, IParser parser) throws IOException {
        try (InputStream inputStream = APITestHelpers.class.getClassLoader().getResourceAsStream("patient_bundle.json")) {
            final Bundle patientBundle = (Bundle) parser.parseResource(inputStream);

            // Post them all
            patientBundle
                    .getEntry()
                    .stream()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .map(resource -> (Patient) resource)
                    .forEach(patient -> {
                        client
                                .create()
                                .resource(patient)
                                .encodedJson()
                                .execute();
                    });
        }
    }

    /**
     * Build Dropwizard test instance with a specific subset of Resources and Providers
     *
     * @param ctx        - {@link FhirContext} context to use
     * @param resources  - {@link List} of resources to add to test instance
     * @param providers  - {@link List} of providers to add to test instance
     * @param validation - {@code true} enable custom validation. {@code false} Disable custom validation
     * @return
     */
    public static ResourceExtension buildResourceExtension(FhirContext
                                                                   ctx, List<Object> resources, List<Object> providers, boolean validation) {

        final var builder = ResourceExtension
                .builder()
                .setRegisterDefaultExceptionMappers(false)
                .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addProvider(new FHIRHandler(ctx))
                .addProvider(FHIRExceptionHandler.class);

        // Optionally enable validation
        if (validation) {
            // Validation config
            final DPCFHIRConfiguration.FHIRValidationConfiguration config = new DPCFHIRConfiguration.FHIRValidationConfiguration();
            config.setEnabled(true);
            config.setSchematronValidation(true);
            config.setSchemaValidation(true);

            final InjectingConstraintValidatorFactory constraintFactory = new InjectingConstraintValidatorFactory(
                    Set.of(new ProfileValidator(new FHIRValidatorProvider(ctx, new DPCProfileSupport(ctx), config).get())));

            builder.setValidator(provideValidator(constraintFactory));
            builder.addProvider(FHIRValidationExceptionHandler.class);
        }

        resources.forEach(builder::addResource);
        providers.forEach(builder::addProvider);

        return builder.build();
    }

    // TODO: Remove as part of DPC-373
    public static IGenericClient buildAuthenticatedClient(FhirContext ctx, String baseURL, String macaroon) {
        final IGenericClient client = ctx.newRestfulGenericClient(baseURL);
        client.registerInterceptor(new MacaroonsInterceptor(macaroon));

        return client;
    }

    static <C extends io.dropwizard.Configuration> void setupApplication(DropwizardTestSupport<C> application) throws
            IOException {
        ConfigFactory.invalidateCaches();
        truncateDatabase();
        application.before();
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

    public static class MacaroonsInterceptor implements IClientInterceptor {

        private String macaroon;

        public MacaroonsInterceptor(String macaroon) {
            this.macaroon = macaroon;
        }

        @Override
        public void interceptRequest(IHttpRequest theRequest) {
            theRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.macaroon);
        }

        @Override
        public void interceptResponse(IHttpResponse theResponse) {
            // Not used
        }

        public String getMacaroon() {
            return macaroon;
        }

        public void setMacaroon(String macaroon) {
            this.macaroon = macaroon;
        }
    }

    static Validator provideValidator(InjectingConstraintValidatorFactory factory) {
        return Validation.byDefaultProvider()
                .configure().constraintValidatorFactory(factory)
                .buildValidatorFactory().getValidator();
    }
}
