package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nitram509.jmacaroons.MacaroonVersion;
import com.github.nitram509.jmacaroons.MacaroonsBuilder;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.dropwizard.handlers.*;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.DefaultFHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.HAPIExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.JerseyExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.PersistenceExceptionHandler;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.ProfileValidator;
import gov.cms.dpc.fhir.validations.dropwizard.FHIRValidatorProvider;
import gov.cms.dpc.fhir.validations.dropwizard.InjectingConstraintValidatorFactory;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.ValidationSupportChain;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.*;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static gov.cms.dpc.api.resources.v1.TokenResource.CLIENT_ASSERTION_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class APITestHelpers {
    private static final String ATTRIBUTION_URL = "http://localhost:3500/v1";
    public static final String ORGANIZATION_ID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0";
    private static final String ATTRIBUTION_TRUNCATE_TASK = "http://localhost:9902/tasks/truncate";
    static final String TASK_URL = "http://localhost:9900/tasks/";
    public static String BASE_URL = "https://dpc.cms.gov/api";

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
                    .forEach(patient -> client
                            .create()
                            .resource(patient)
                            .encodedJson()
                            .execute());
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

        final var builder = ResourceExtension
                .builder()
                .setRegisterDefaultExceptionMappers(false)
                .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addProvider(new FHIRHandler(ctx))
                .addProvider(JerseyExceptionHandler.class)
                .addProvider(PersistenceExceptionHandler.class)
                .addProvider(HAPIExceptionHandler.class)
                .addProvider(DefaultFHIRExceptionHandler.class);

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

    public static IGenericClient buildAuthenticatedClient(FhirContext ctx, String baseURL, String macaroon, String keyID, PrivateKey privateKey) throws IOException, URISyntaxException {


        final AuthResponse authResponse = jwtAuthFlow(baseURL, macaroon, keyID, privateKey);
        // Request an access token from the JWT endpoint
        final IGenericClient client = ctx.newRestfulGenericClient(baseURL);
        client.registerInterceptor(new MacaroonsInterceptor(authResponse.accessToken));

        return client;
    }

    public static AuthResponse jwtAuthFlow(String baseURL, String macaroon, String keyID, PrivateKey privateKey) throws IOException, URISyntaxException {
        final String jwt = Jwts.builder()
                .setHeaderParam("kid", keyID)
                .setAudience(String.format("%sToken/auth", baseURL))
                .setIssuer(macaroon)
                .setSubject(macaroon)
                .setId(UUID.randomUUID().toString())
                .setExpiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                .signWith(privateKey, SignatureAlgorithm.RS384)
                .compact();

        // Submit JWT to /auth endpoint
        final AuthResponse authResponse;
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/Token/auth", baseURL));
            builder.addParameter("scope", "Nothing");
            builder.addParameter("grant_type", "client_credentials");
            builder.addParameter("client_assertion_type", CLIENT_ASSERTION_TYPE);
            builder.addParameter("client_assertion", jwt);
            final HttpPost post = new HttpPost(builder.build());
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Token request should have succeeded");
                authResponse = mapper.readValue(response.getEntity().getContent(), AuthResponse.class);
                assertNotEquals(macaroon, authResponse.accessToken, "New Macaroon should not be identical");
                assertEquals(300, authResponse.expiresIn, "Should expire in 300 seconds");
            }
        }
        return authResponse;
    }

    static String createGoldenMacaroon() throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(String.format("%s/generate-token", TASK_URL));

            try (CloseableHttpResponse execute = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, execute.getStatusLine().getStatusCode(), "Generated macaroon");
                return EntityUtils.toString(execute.getEntity());
            }
        }
    }

    /**
     * Generate a new {@link KeyPair} and submit the {@link PublicKey} to the API service, along with the given label
     *
     * @param keyID          - {@link String} identifier (kid) of the public key
     * @param organizationID - {@link String} organization ID to register key with
     * @param goldenMacaroon - {@link String} admin Macaroon that can upload keys
     * @param baseURL        - {@link String} baseURl to submit Key to
     * @return - {@link PrivateKey} which matches uploaded {@link PublicKey}
     * @throws IOException              - throws if something bad happens
     * @throws URISyntaxException       - throws if the URI is no good
     * @throws NoSuchAlgorithmException - throws if security breaks
     */
    public static PrivateKey generateAndUploadKey(String keyID, String organizationID, String goldenMacaroon, String baseURL) throws IOException, URISyntaxException, NoSuchAlgorithmException {
        final KeyPair keyPair = generateKeyPair();
        final String key = generatePublicKey(keyPair.getPublic());

        // Create org specific macaroon from Golden Macaroon
        final String macaroon = MacaroonsBuilder
                .modify(MacaroonsBuilder.deserialize(goldenMacaroon).get(0))
                .add_first_party_caveat(String.format("organization_id = %s", organizationID))
                .getMacaroon().serialize(MacaroonVersion.SerializationVersion.V2_JSON);

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final URIBuilder builder = new URIBuilder(String.format("%s/Key", baseURL));
            builder.addParameter("label", keyID);
            final HttpPost post = new HttpPost(builder.build());
            post.setEntity(new StringEntity(key));
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + macaroon);
            post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode(), "Key should be valid");
            }
        }

        return keyPair.getPrivate();
    }

    static <C extends io.dropwizard.Configuration> void setupApplication(DropwizardTestSupport<C> application) throws
            Exception {
        ConfigFactory.invalidateCaches();
        // Truncate attribution database
        truncateDatabase();
        application.before();
        // Truncate the Auth DB
        application.getApplication().run("db", "drop-all", "--confirm-delete-everything");
        application.getApplication().run("db", "migrate");

    }

    private static void truncateDatabase() throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost post = new HttpPost(ATTRIBUTION_TRUNCATE_TASK);

            try (CloseableHttpResponse execute = client.execute(post)) {
                assertEquals(HttpStatus.OK_200, execute.getStatusLine().getStatusCode(), "Should have truncated database");
            }
        }
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        return kpg.generateKeyPair();
    }

    public static String generatePublicKey(PublicKey key) {
        final String encoded = Base64.getMimeEncoder().encodeToString(key.getEncoded());
        return String.format("-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----\n", encoded);
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

        public String getMacaroon() {
            return macaroon;
        }

        public void setMacaroon(String macaroon) {
            this.macaroon = macaroon;
        }
    }

    private static Validator provideValidator(InjectingConstraintValidatorFactory factory) {
        return Validation.byDefaultProvider()
                .configure().constraintValidatorFactory(factory)
                .buildValidatorFactory().getValidator();
    }

    @SuppressWarnings("WeakerAccess")
    public static class AuthResponse {

        @JsonProperty(value = "access_token")
        public String accessToken;
        @JsonProperty(value = "token_type")
        public String tokenType;
        @JsonProperty(value = "expires_in")
        public Long expiresIn;
        public String scope;

        public AuthResponse() {
            // Not used
        }
    }
}
