package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRValidationExceptionHandler;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.ProfileValidator;
import gov.cms.dpc.fhir.validations.dropwizard.FHIRValidatorProvider;
import gov.cms.dpc.fhir.validations.dropwizard.InjectingConstraintValidatorFactory;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.hl7.fhir.dstu3.model.Bundle;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class APITestHelpers {
    public static String BASE_URL = "https://dpc.cms.gov/fhir";
    public static String ATTRIBUTION_URL = "http://localhost:3500/v1";
    public static final String ORGANIZATION_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8254";

    private APITestHelpers() {
        // Not used
    }

    public static String setupOrganizationTest(FhirContext ctx) throws IOException {
        // Register an organization, and a token
        // Read in the test file
        String macaroon;
        try (InputStream inputStream = APITestHelpers.class.getClassLoader().getResourceAsStream("organization.tmpl.json")) {
            final IParser parser = ctx.newJsonParser();
            final Bundle orgBundle = (Bundle) parser.parseResource(inputStream);
            final String bundleString = parser.encodeResourceToString(orgBundle);
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                final HttpPost httpPost = new HttpPost(ATTRIBUTION_URL + "/Organization");
                httpPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);
                httpPost.setEntity(new StringEntity(bundleString));

                try (CloseableHttpResponse response = client.execute(httpPost)) {
                    assertEquals(HttpStatus.CREATED_201, response.getStatusLine().getStatusCode(), "Should have succeeded");
                }

                // Now, create a Macaroon
                final HttpPost tokenPost = new HttpPost(String.format("%s/Organization/%s/token", ATTRIBUTION_URL, ORGANIZATION_ID));
                tokenPost.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

                try (CloseableHttpResponse response = client.execute(tokenPost)) {
                    assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Should have succeeded");
                    macaroon = EntityUtils.toString(response.getEntity());
                    assertNotNull(macaroon, "Should have Macaroon");
                }
            }
        }

        return macaroon;
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
    public static ResourceExtension buildResourceExtension(FhirContext ctx, List<Object> resources, List<Object> providers, boolean validation) {

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

    public static class MacaroonsInterceptor implements IClientInterceptor {

        private final String macaroon;

        MacaroonsInterceptor(String macaroon) {
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
    }

    static Validator provideValidator(InjectingConstraintValidatorFactory factory) {
        return Validation.byDefaultProvider()
                .configure().constraintValidatorFactory(factory)
                .buildValidatorFactory().getValidator();
    }
}
