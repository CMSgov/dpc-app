package gov.cms.dpc.api.auth.jwt;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.auth.DPCAuthDynamicFeature;
import gov.cms.dpc.api.auth.DPCAuthFactory;
import gov.cms.dpc.api.auth.macaroonauth.MacaroonsAuthenticator;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.api.resources.v1.TokenResource;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.DPCValidationErrorMessage;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Token Resource Unit Tests")
@SuppressWarnings("InnerClassMayBeStatic")
class JWTUnitTests {

    private static final ResourceExtension RESOURCE = buildResources();
    private static final Map<UUID, KeyPair> JWTKeys = new HashMap<>();
    private static final UUID correctKEYID = UUID.randomUUID();

    private JWTUnitTests() {
        try {
            JWTKeys.put(correctKEYID, APIAuthHelpers.generateKeyPair());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Nested
    @DisplayName("Query Param tests")
    class QueryParmTests {

        @Test
        void testQueryParams() {
            final String payload = "this is not a payload";
            Response response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.entity(payload, MediaType.APPLICATION_FORM_URLENCODED));

            // Should have all exceptions
            DPCValidationErrorMessage validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(400, response.getStatus(), "Should have failed");
            assertEquals(3, validationErrorResponse.getErrors().size(), "Should have three validations");

            // Add the missing scope value and try again
            response = RESOURCE.target("/v1/Token/auth").queryParam("scope", "system/*.*")
                    .request()
                    .post(Entity.entity(payload, MediaType.APPLICATION_FORM_URLENCODED));

            // Should have one less exception
            validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(400, response.getStatus(), "Should have failed");
            assertEquals(2, validationErrorResponse.getErrors().size(), "Should have two validations");


            // Add the grant type
            response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .request()
                    .post(Entity.entity(payload, MediaType.APPLICATION_FORM_URLENCODED));

            // Should have all exceptions
            validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(400, response.getStatus(), "Should have failed");
            assertEquals(1, validationErrorResponse.getErrors().size(), "Should only have a single violation");
            assertTrue(validationErrorResponse.getErrors().get(0).contains("Assertion type is required"));

            // Add the assertion type and try again
            response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .request()
                    .post(Entity.entity(payload, MediaType.APPLICATION_FORM_URLENCODED));

            // Should have no validation exceptions, but still fail
            assertEquals(500, response.getStatus(), "Should have failed, but for different reasons");
        }

        @Test
        void testInvalidGrantTypeValue() {
            final String payload = "not a real payload";
            Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "wrong_grant_type")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .request()
                    .post(Entity.entity(payload, MediaType.APPLICATION_FORM_URLENCODED));

            assertEquals(400, response.getStatus(), "Should have failed, but for different reasons");
            assertTrue(response.readEntity(String.class).contains("Grant Type must be 'client_credentials'"), "Should have correct exception");
        }

        @Test
        void testEmptyGrantTypeValue() {
            final Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .request()
                    .post(Entity.entity("payload", MediaType.APPLICATION_FORM_URLENCODED));

            // Setting the grant type to be blank, should throw a validation error
            assertEquals(400, response.getStatus(), "Should have failed, but for different reasons");
            assertNotNull(response.readEntity(DPCValidationErrorMessage.class), "Should have a validation failure");
        }

        @Test
        void testInvalidClientAssertionType() {
            final String payload = "not a real payload";
            Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", "Not a real assertion_type")
                    .request()
                    .post(Entity.entity(payload, MediaType.APPLICATION_FORM_URLENCODED));

            assertEquals(400, response.getStatus(), "Should have failed, but for different reasons");
            assertTrue(response.readEntity(String.class).contains("Client Assertion Type must be 'urn:ietf:params:oauth:client-assertion-type:jwt-bearer'"), "Should have correct error message");
        }

        @Test
        void testEmptyClientAssertionType() {
            final Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", "")
                    .request()
                    .post(Entity.entity("payload", MediaType.APPLICATION_FORM_URLENCODED));

            // Setting the assertion type to be blank, should throw a validation error
            assertEquals(400, response.getStatus(), "Should have failed, but for different reasons");
            assertNotNull(response.readEntity(DPCValidationErrorMessage.class), "Should have a validation failure");
        }

        @Test
        void testInvalidScopeType() {
            final String payload = "not a real payload";
            Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "this is not a scope")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                    .request()
                    .post(Entity.entity(payload, MediaType.APPLICATION_FORM_URLENCODED));

            assertEquals(400, response.getStatus(), "Should have failed, but for different reasons");
            assertTrue(response.readEntity(String.class).contains("Access Scope must be 'system/*.*'"), "Should have correct error message");
        }

        @Test
        void testEmptyScopeType() {
            final Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                    .request()
                    .post(Entity.entity("payload", MediaType.APPLICATION_FORM_URLENCODED));

            // Setting the assertion type to be blank, should throw a validation error
            assertEquals(400, response.getStatus(), "Should have failed, but for different reasons");
            assertNotNull(response.readEntity(DPCValidationErrorMessage.class), "Should have a validation failure");
        }
    }


    @Nested
    @DisplayName("JWT Tests")
    class JWTests {

        @Test
        void testMissingJWTPublicKey() throws NoSuchAlgorithmException {
            // Submit JWT with missing key
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair();

            final String jwt = Jwts.builder()
                    .setHeaderParam("kid", UUID.randomUUID())
                    .setAudience(String.format("%sToken/auth", "here"))
                    .setIssuer("macaroon")
                    .setSubject("macaroon")
                    .setId(UUID.randomUUID().toString())
                    .setExpiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS384)
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .queryParam("client_assertion", jwt)
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED));

            assertEquals(401, response.getStatus(), "Should be unauthorized");
            assertTrue(response.readEntity(String.class).contains("Cannot find public key"), "Should have correct exception");
        }

        @Test
        void testExpiredJWT() {
            final KeyPair keyPair = JWTKeys.get(correctKEYID);

            final String jwt = Jwts.builder()
                    .setHeaderParam("kid", correctKEYID)
                    .setAudience(String.format("%sToken/auth", "here"))
                    .setIssuer("macaroon")
                    .setSubject("macaroon")
                    .setId(UUID.randomUUID().toString())
                    .setExpiration(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS384)
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .queryParam("client_assertion", jwt)
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED));

            assertEquals(401, response.getStatus(), "Should be unauthorized");
            assertTrue(response.readEntity(String.class).contains("Invalid JWT"), "Should have correct exception");
        }

        @Test
        void testJWTWrongSigningKey() throws NoSuchAlgorithmException {
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair();

            final String jwt = Jwts.builder()
                    .setHeaderParam("kid", correctKEYID)
                    .setAudience(String.format("%sToken/auth", "here"))
                    .setIssuer("macaroon")
                    .setSubject("macaroon")
                    .setId(UUID.randomUUID().toString())
                    .setExpiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS384)
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .queryParam("client_assertion", jwt)
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED));

            assertEquals(401, response.getStatus(), "Should be unauthorized");
            assertTrue(response.readEntity(String.class).contains("Invalid JWT"), "Should have correct exception");
        }

        @Test
        void testJTIReplay() {
            final KeyPair keyPair = JWTKeys.get(correctKEYID);

            final String jwt = Jwts.builder()
                    .setHeaderParam("kid", correctKEYID)
                    .setAudience(String.format("%sToken/auth", "localhost:3002/v1/"))
                    .setIssuer("macaroon")
                    .setSubject("macaroon")
                    .setId(UUID.randomUUID().toString())
                    .setExpiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS384)
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .queryParam("client_assertion", jwt)
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED));

            // Since we're not using valid Macaroons for testing, we should get a 500 error, which means we made it past the JWT stage
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.getStatus(), "Should have invalid Macaroon");

            // Try to submit again
            Response r2 = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .queryParam("client_assertion", jwt)
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED));

            assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED_401, r2.getStatus(), "Should be unauthorized"),
                    () -> assertTrue(r2.readEntity(String.class).contains("Invalid JWT"), "Should have invalid JWT"));
        }
    }

    private static ResourceExtension buildResources() {
        final IGenericClient client = mock(IGenericClient.class);
        final MacaroonBakery bakery = buildBakery();
        final TokenDAO tokenDAO = mock(TokenDAO.class);
        final PublicKeyDAO publicKeyDAO = mockKeyDAO();
        Mockito.when(tokenDAO.fetchTokens(Mockito.any())).thenAnswer(answer -> "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0");

        final JwtKeyResolver resolver = new JwtKeyResolver(publicKeyDAO);
        final CaffeineJTICache jtiCache = new CaffeineJTICache();

        final TokenPolicy tokenPolicy = new TokenPolicy();

        final DPCAuthFactory factory = new DPCAuthFactory(bakery, new MacaroonsAuthenticator(client), tokenDAO);
        final DPCAuthDynamicFeature dynamicFeature = new DPCAuthDynamicFeature(factory);

        final TokenResource tokenResource = new TokenResource(tokenDAO, bakery, tokenPolicy, resolver, jtiCache, "localhost:3002/v1");
        final FhirContext ctx = FhirContext.forDstu3();

        return APITestHelpers.buildResourceExtension(ctx, List.of(tokenResource), List.of(dynamicFeature), false);
    }

    private static MacaroonBakery buildBakery() {
        return new MacaroonBakery.MacaroonBakeryBuilder("http://test.local",
                new MemoryRootKeyStore(new SecureRandom()),
                new MemoryThirdPartyKeyStore()).build();
    }

    private static PublicKeyDAO mockKeyDAO() {
        final PublicKeyDAO mock = mock(PublicKeyDAO.class);

        Mockito.when(mock.fetchPublicKey(Mockito.any(), Mockito.any())).then(answer -> {
            @SuppressWarnings("RedundantCast") final KeyPair keyPair = JWTKeys.get((UUID) answer.getArgument(0));
            if (keyPair == null) {
                return Optional.empty();
            }
            final PublicKeyEntity entity = new PublicKeyEntity();
            entity.setPublicKey(SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));
            return Optional.of(entity);
        });
        return mock;
    }
}
