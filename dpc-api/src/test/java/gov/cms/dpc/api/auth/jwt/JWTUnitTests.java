package gov.cms.dpc.api.auth.jwt;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.github.nitram509.jmacaroons.MacaroonVersion;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.auth.DPCAuthDynamicFeature;
import gov.cms.dpc.api.auth.DPCAuthFactory;
import gov.cms.dpc.api.auth.DPCUnauthorizedHandler;
import gov.cms.dpc.api.auth.MacaroonHelpers;
import gov.cms.dpc.api.auth.macaroonauth.MacaroonsAuthenticator;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.api.resources.v1.TokenResource;
import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.DPCValidationErrorMessage;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.KeyType;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import io.jsonwebtoken.Jwts;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Token Resource Unit Tests")
@SuppressWarnings("InnerClassMayBeStatic")
class JWTUnitTests {

    private static final ResourceExtension RESOURCE = buildResources();
    private static final Map<UUID, KeyPair> JWTKeys = new HashMap<>();

    private JWTUnitTests() {
    }

    @AfterEach
    void cleanup() {
        JWTKeys.clear();
    }

    @Nested
    @DisplayName("Form Param Tests")
    class FormParamTests {

        @Test
        void testFormParams() {
            final String payload = "this is not a payload";
            final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();

            Response response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should have all exceptions
            DPCValidationErrorMessage validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(400, response.getStatus(), "Should have failed");
            assertEquals(4, validationErrorResponse.getErrors().size(), "Should have four violations");

            formData.add("scope", "system/*.*");
            // Add the missing scope value and try again
            response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should have one less exception
            validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(400, response.getStatus(), "Should have failed");
            assertEquals(3, validationErrorResponse.getErrors().size(), "Should have three violations");

            formData.add("grant_type", "client_credentials");


            // Add the grant type
            response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should still have an exception
            validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(400, response.getStatus(), "Should have failed");
            assertEquals(2, validationErrorResponse.getErrors().size(), "Should have two violation");

            // Add the assertion type
            formData.add("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE);
            response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should another for the empty client_assertion
            validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(400, response.getStatus(), "Should have a 400 from an empty param");
            assertEquals(1, validationErrorResponse.getErrors().size(), "Should only have a single violation");
            assertTrue(validationErrorResponse.getErrors().get(0).contains("Assertion is required"));

            // Add the token and try again
            formData.add("client_assertion", payload);

            response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should have no validation exceptions, but still fail
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should be unauthorized");
        }

        @Test
        void testInvalidGrantTypeValue() {
            final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
            formData.add("scope", "system/*.*");
            formData.add("grant_type", "wrong_grant_type");
            formData.add("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE);
            formData.add("client_assertion", "dummyJWT");

            Response response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            assertEquals(400, response.getStatus(), "Should have failed, but for different reasons");
            assertTrue(response.readEntity(String.class).contains("Grant Type must be 'client_credentials'"), "Should have correct exception");
        }

        @Test
        void testEmptyGrantTypeValue() {
            final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
            formData.add("scope", "system/*.*");
            formData.add("grant_type", "");
            formData.add("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE);
            formData.add("client_assertion", "dummyJWT");
            final Response response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Setting the grant type to be blank, should throw a validation error
            assertEquals(400, response.getStatus(), "Should have failed");
            DPCValidationErrorMessage errorMessage = response.readEntity(DPCValidationErrorMessage.class);
            assertNotNull(errorMessage, "Should have a validation failure");
            assertEquals("arg1 Grant type is required", errorMessage.getErrors().get(0), "Should fail due to missing grant type");
        }

        @Test
        void testInvalidClientAssertionType() {
            final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
            formData.add("scope", "system/*.*");
            formData.add("grant_type", "client_credentials");
            formData.add("client_assertion_type", "Not a real assertion_type");
            formData.add("client_assertion", "dummyJWT");

            Response response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            assertEquals(400, response.getStatus(), "Should have failed, but for different reasons");
            assertTrue(response.readEntity(String.class).contains("Client Assertion Type must be 'urn:ietf:params:oauth:client-assertion-type:jwt-bearer'"), "Should have correct error message");
        }

        @Test
        void testEmptyClientAssertionType() {
            final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
            formData.add("scope", "system/*.*");
            formData.add("grant_type", "client_credentials");
            formData.add("client_assertion_type", "");
            formData.add("client_assertion", "dummyJWT");

            final Response response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Setting the assertion type to be blank, should throw a validation error
            assertEquals(400, response.getStatus(), "Should have failed, but for different reasons");
            DPCValidationErrorMessage errorMessage = response.readEntity(DPCValidationErrorMessage.class);
            assertNotNull(errorMessage, "Should have a validation failure");
            assertEquals("arg2 Assertion type is required", errorMessage.getErrors().get(0), "Should fail due to assertion type");
        }

        @Test
        void testInvalidScopeType() {
            final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
            formData.add("scope", "this is not a scope");
            formData.add("grant_type", "client_credentials");
            formData.add("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE);
            formData.add("client_assertion", "dummyJWT");

            Response response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            assertEquals(400, response.getStatus(), "Should have failed, but for different reasons");
            assertTrue(response.readEntity(String.class).contains("Access Scope must be 'system/*.*'"), "Should have correct error message");
        }

        @Test
        void testEmptyScopeType() {
            final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
            formData.add("scope", "");
            formData.add("grant_type", "client_credentials");
            formData.add("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE);
            formData.add("client_assertion", "dummyJWT");

            final Response response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Setting the assertion type to be blank, should throw a validation error
            assertEquals(400, response.getStatus(), "Should have failed, but for different reasons");
            assertNotNull(response.readEntity(DPCValidationErrorMessage.class), "Should have a validation failure");
        }
    }


    @Nested
    @DisplayName("JWT Tests")
    @Disabled //TODO: debug - figure out how to set public key org id to match macaroon's
    class JWTests {

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testMissingJWTPublicKey(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with missing key
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            String macaroon = buildMacaroon();

            final String jwt = Jwts.builder()
                    .header().add("kid", UUID.randomUUID().toString()).and()
                    .audience().add("localhost:3002/v1/Token/auth").and()
                    .issuer(macaroon)
                    .subject(macaroon)
                    .id(UUID.randomUUID().toString())
                    .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getRight(), APIAuthHelpers.getSigningAlgorithm(keyType))
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

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testExpiredJWT(KeyType keyType) throws NoSuchAlgorithmException {
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            String macaroon = buildMacaroon();

            final String jwt = Jwts.builder()
                    .header().add("kid", keyPair.getLeft()).and()
                    .audience().add(String.format("%sToken/auth", "here")).and()
                    .issuer(macaroon)
                    .subject(macaroon)
                    .id(UUID.randomUUID().toString())
                    .expiration(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getRight(), APIAuthHelpers.getSigningAlgorithm(keyType))
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

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testJWTWrongSigningKey(KeyType keyType) throws NoSuchAlgorithmException {
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final String jwt = Jwts.builder()
                    .header().add("kid", keyPair.getLeft()).and()
                    .audience().add(String.format("%sToken/auth", "here")).and()
                    .issuer("macaroon")
                    .subject("macaroon")
                    .id(UUID.randomUUID().toString())
                    .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getRight(), APIAuthHelpers.getSigningAlgorithm(keyType))
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

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testJTIReplay(KeyType keyType) throws NoSuchAlgorithmException {
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            String macaroon = buildMacaroon();

            final String jwt = Jwts.builder()
                    .header().add("kid", keyPair.getLeft()).and()
                    .audience().add(String.format("%sToken/auth", "localhost:3002/v1/")).and()
                    .issuer(macaroon)
                    .subject(macaroon)
                    .id(UUID.randomUUID().toString())
                    .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getRight(), APIAuthHelpers.getSigningAlgorithm(keyType))
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .queryParam("client_assertion", jwt)
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED));

            assertEquals(HttpStatus.OK_200, response.getStatus(), "Should get a good status on first request");

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

    @Nested
    @DisplayName("JWT Validation Tests")
    class ValidationTests {

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testNonToken(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final String jwt = Jwts.builder()
                    .header().add("kid", UUID.randomUUID().toString()).and()
                    .audience().add("localhost:3002/v1/Token/auth").and()
                    .issuer("macaroon")
                    .subject("macaroon")
                    .id(UUID.randomUUID().toString())
                    .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getRight(), APIAuthHelpers.getSigningAlgorithm(keyType))
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(400, response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("Client token is not formatted correctly"), "Should have correct exception");
        }

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testUUIDToken(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final String id = UUID.randomUUID().toString();
            final String jwt = Jwts.builder()
                    .header().add("kid", UUID.randomUUID().toString()).and()
                    .audience().add("localhost:3002/v1/Token/auth").and()
                    .issuer(id)
                    .subject(id)
                    .id(id)
                    .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getRight(), APIAuthHelpers.getSigningAlgorithm(keyType))
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(400, response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("Cannot use Token ID as `client_token`, must use actual token value"), "Should have correct exception");
        }

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testExpiredJWT(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);

            final String id = UUID.randomUUID().toString();
            final String jwt = Jwts.builder()
                    .header().add("kid", UUID.randomUUID().toString()).and()
                    .audience().add(String.format("%sToken/auth", "here")).and()
                    .issuer(id)
                    .subject(id)
                    .id(id)
                    .expiration(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getPrivate(), APIAuthHelpers.getSigningAlgorithm(keyType))
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(400, response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("JWT is expired"), "Should have correct exception");
        }

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testNumericExpiration(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);

            final Map<String, Object> claims = new HashMap<>();
            claims.put("exp", "-100");

            final String id = UUID.randomUUID().toString();
            final String jwt = Jwts.builder()
                    .header().add("kid", UUID.randomUUID().toString()).and()
                    .audience().add(String.format("%sToken/auth", "here")).and()
                    .issuer(id)
                    .subject(id)
                    .id(id)
                    .claims().add(claims).and()
                    .signWith(keyPair.getPrivate(), APIAuthHelpers.getSigningAlgorithm(keyType))
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(400, response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("JWT is expired"), "Should have correct exception");
        }

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testOverlongJWT(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);

            final String id = UUID.randomUUID().toString();
            final String jwt = Jwts.builder()
                    .header().add("kid", UUID.randomUUID().toString()).and()
                    .audience().add(String.format("%sToken/auth", "here")).and()
                    .issuer(id)
                    .subject(id)
                    .id(id)
                    .expiration(Date.from(Instant.now().plus(12, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getPrivate(), APIAuthHelpers.getSigningAlgorithm(keyType))
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(400, response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("Token expiration cannot be more than 5 minutes in the future"), "Should have correct exception");
        }

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testWrongAudClaim(KeyType keyType) throws NoSuchAlgorithmException {
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);
            final String m = buildMacaroon();

            final String id = UUID.randomUUID().toString();
            final String jwt = Jwts.builder()
                    .header().add("kid", UUID.randomUUID().toString()).and()
                    .audience().add(String.format("%sToken/auth", "here")).and()
                    .issuer(m)
                    .subject(m)
                    .id(id)
                    .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getPrivate(), APIAuthHelpers.getSigningAlgorithm(keyType))
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(400, response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("Audience claim value is incorrect"), "Should have correct exception");
        }

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testSuccess(KeyType keyType) throws NoSuchAlgorithmException {
            final String m = buildMacaroon();
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);

            final String id = UUID.randomUUID().toString();
            final String jwt = Jwts.builder()
                    .header().add("kid", UUID.randomUUID().toString()).and()
                    .audience().add("localhost:3002/v1/Token/auth").and()
                    .issuer(m)
                    .subject(m)
                    .id(id)
                    .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getPrivate(), APIAuthHelpers.getSigningAlgorithm(keyType))
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(200, response.getStatus(), "Should be valid");
        }

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testMismatchClaims(KeyType keyType) throws NoSuchAlgorithmException {
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);

            final String id = UUID.randomUUID().toString();
            final String jwt = Jwts.builder()
                    .header().add("kid", UUID.randomUUID().toString()).and()
                    .audience().add(String.format("%sToken/auth", "here")).and()
                    .issuer("this is")
                    .subject("not matching")
                    .id(id)
                    .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getPrivate(), APIAuthHelpers.getSigningAlgorithm(keyType))
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(400, response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("Issuer and Subject must be identical"), "Should have correct exception");
        }

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testMissingClaim(KeyType keyType) throws NoSuchAlgorithmException {
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);

            final String id = UUID.randomUUID().toString();
            final String jwt = Jwts.builder()
                    .header().add("kid", UUID.randomUUID().toString()).and()
                    .audience().add(String.format("%sToken/auth", "here")).and()
                    .subject("not matching")
                    .id(id)
                    .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getPrivate(), APIAuthHelpers.getSigningAlgorithm(keyType))
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(400, response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("Claim `issuer` must be present"), "Should have correct exception");
        }

        @Test
        void testNotJWT() {
            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity("this is not a jwt", MediaType.TEXT_PLAIN));

            assertEquals(400, response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("JWT is not formatted correctly"), "Should have correct exception");
        }

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testMissingKID(KeyType keyType) throws NoSuchAlgorithmException {
            final String m = buildMacaroon();
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);

            final String id = UUID.randomUUID().toString();
            final String jwt = Jwts.builder()
                    .audience().add("localhost:3002/v1/Token/auth").and()
                    .issuer(m)
                    .subject(m)
                    .id(id)
                    .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getPrivate(), APIAuthHelpers.getSigningAlgorithm(keyType))
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(400, response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("JWT header must have `kid` value"), "Should have correct exception");
        }

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testInvalidKID(KeyType keyType) throws NoSuchAlgorithmException {
            final String m = buildMacaroon();
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);

            final String id = UUID.randomUUID().toString();
            final String jwt = Jwts.builder()
                    .audience().add("localhost:3002/v1/Token/auth").and()
                    .header().add("kid", "this is not a kid").and()
                    .issuer(m)
                    .subject(m)
                    .id(id)
                    .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .signWith(keyPair.getPrivate(), APIAuthHelpers.getSigningAlgorithm(keyType))
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(400, response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("`kid` value must be a UUID"), "Should have correct exception");
        }

        @Disabled //TODO: can we even hit this exception anymore? -acw
        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testIncorrectExpFormat(KeyType keyType) throws NoSuchAlgorithmException {
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);
            final String m = buildMacaroon();

            final Map<String, Object> claims = new HashMap<>();
            claims.put("exp", "asdf");

            final String id = UUID.randomUUID().toString();
            final String jwt = Jwts.builder()
                    .header().add("kid", UUID.randomUUID().toString()).and()
                    .audience().add("localhost:3002/v1/Token/auth").and()
                    .issuer(m)
                    .subject(m)
                    .id(id)
                    .claims().add(claims).and()
                    .signWith(keyPair.getRight(), APIAuthHelpers.getSigningAlgorithm(keyType))
                    .compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(400, response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("Expiration time must be seconds since unix epoch"), "Should have correct exception");
        }
    }

    private static Pair<String, PrivateKey> generateKeypair(KeyType keyType) throws NoSuchAlgorithmException {
        final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);
        final UUID uuid = UUID.randomUUID();
        JWTKeys.put(uuid, keyPair);
        return Pair.of(uuid.toString(), keyPair.getPrivate());

    }

    private static ResourceExtension buildResources() {
        final IGenericClient client = mock(IGenericClient.class);
        final MacaroonBakery bakery = buildBakery();
        final TokenDAO tokenDAO = mock(TokenDAO.class);
        final PublicKeyDAO publicKeyDAO = mockKeyDAO();
        final DPCUnauthorizedHandler dpc401handler = mock(DPCUnauthorizedHandler.class);
        Mockito.when(tokenDAO.fetchTokens(Mockito.any())).thenAnswer(answer -> "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0");

        final JwtKeyLocator locator = spy(new JwtKeyLocator(publicKeyDAO));
        final CaffeineJTICache jtiCache = new CaffeineJTICache();

        final TokenPolicy tokenPolicy = new TokenPolicy();

        final DPCAuthFactory factory = new DPCAuthFactory(bakery, new MacaroonsAuthenticator(client), tokenDAO, dpc401handler);
        final DPCAuthDynamicFeature dynamicFeature = new DPCAuthDynamicFeature(factory);

        final TokenResource tokenResource = spy(new TokenResource(tokenDAO, bakery, tokenPolicy, locator, jtiCache, "localhost:3002/v1"));

        UUID organizationID = UUID.randomUUID();
        doReturn(organizationID).when(tokenResource).getOrganizationID(Mockito.anyString());

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

        Mockito.when(mock.fetchPublicKey(Mockito.any())).then(answer -> {
            final KeyPair keyPair = JWTKeys.get((UUID) answer.getArgument(0));
            if (keyPair == null) {
                return Optional.empty();
            }
            final PublicKeyEntity entity = new PublicKeyEntity();
            entity.setPublicKey(SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));
            return Optional.of(entity);
        });
        return mock;
    }

    private static String buildMacaroon() {
        MacaroonBakery bakery = buildBakery();

        MacaroonCondition orgCondition = new MacaroonCondition(
                MacaroonHelpers.ORGANIZATION_CAVEAT_KEY,
                MacaroonCondition.Operator.EQ,
                UUID.randomUUID().toString());
        MacaroonCaveat orgCaveat = new MacaroonCaveat(orgCondition);

        return bakery.createMacaroon(List.of(orgCaveat))
                .serialize(MacaroonVersion.SerializationVersion.V2_JSON);
    }
}
