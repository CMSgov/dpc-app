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
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Token resource processing")
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
        @DisplayName("Form validation error handling 🤮")
        void testFormParams() {
            final String payload = "this is not a payload";
            final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();

            Response response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should have all exceptions
            DPCValidationErrorMessage validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have failed");
            assertEquals(4, validationErrorResponse.getErrors().size(), "Should have four violations");

            formData.add("scope", "system/*.*");
            // Add the missing scope value and try again
            response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should have one less exception
            validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have failed");
            assertEquals(3, validationErrorResponse.getErrors().size(), "Should have three violations");

            formData.add("grant_type", "client_credentials");


            // Add the grant type
            response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should still have an exception
            validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have failed");
            assertEquals(2, validationErrorResponse.getErrors().size(), "Should have two violation");

            // Add the assertion type
            formData.add("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE);
            response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should another for the empty client_assertion
            validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have a 400 from an empty param");
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
        @DisplayName("Invalid grant type value 🤮")
        void testInvalidGrantTypeValue() {
            final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
            formData.add("scope", "system/*.*");
            formData.add("grant_type", "wrong_grant_type");
            formData.add("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE);
            formData.add("client_assertion", "dummyJWT");

            Response response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have failed, but for different reasons");
            assertTrue(response.readEntity(String.class).contains("Grant Type must be 'client_credentials'"), "Should have correct exception");
        }

        @Test
        @DisplayName("Empty grant type value 🤮")
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
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have failed");
            DPCValidationErrorMessage errorMessage = response.readEntity(DPCValidationErrorMessage.class);
            assertNotNull(errorMessage, "Should have a validation failure");
            assertEquals("arg1 Grant type is required", errorMessage.getErrors().get(0), "Should fail due to missing grant type");
        }

        @Test
        @DisplayName("Invalid client assertion type 🤮")
        void testInvalidClientAssertionType() {
            final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
            formData.add("scope", "system/*.*");
            formData.add("grant_type", "client_credentials");
            formData.add("client_assertion_type", "Not a real assertion_type");
            formData.add("client_assertion", "dummyJWT");

            Response response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have failed, but for different reasons");
            assertTrue(response.readEntity(String.class).contains("Client Assertion Type must be 'urn:ietf:params:oauth:client-assertion-type:jwt-bearer'"), "Should have correct error message");
        }

        @Test
        @DisplayName("Empty client assertion type 🤮")
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
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have failed, but for different reasons");
            DPCValidationErrorMessage errorMessage = response.readEntity(DPCValidationErrorMessage.class);
            assertNotNull(errorMessage, "Should have a validation failure");
            assertEquals("arg2 Assertion type is required", errorMessage.getErrors().get(0), "Should fail due to assertion type");
        }

        @Test
        @DisplayName("Invalid scope type 🤮")
        void testInvalidScopeType() {
            final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
            formData.add("scope", "this is not a scope");
            formData.add("grant_type", "client_credentials");
            formData.add("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE);
            formData.add("client_assertion", "dummyJWT");

            Response response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have failed, but for different reasons");
            assertTrue(response.readEntity(String.class).contains("Access Scope must be 'system/*.*'"), "Should have correct error message");
        }

        @Test
        @DisplayName("Empty scope type 🤮")
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
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have failed, but for different reasons");
            assertNotNull(response.readEntity(DPCValidationErrorMessage.class), "Should have a validation failure");
        }
    }


    @Nested
    @DisplayName("JWT Tests")
    class JWTests {

        @ParameterizedTest
        @DisplayName("JWT public key not found in key store 🤮")
        @EnumSource(KeyType.class)
        void testMissingJWTPublicKey(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with missing key
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);

            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", UUID.randomUUID().toString());
            builder.audience().add(String.format("%sToken/auth", "here"));
            builder.issuer("macaroon");
            builder.subject("macaroon");
            builder.id(UUID.randomUUID().toString());
            builder.expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getPrivate());
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .queryParam("client_assertion", jwt)
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED));

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should be unauthorized");
            String x = response.readEntity(String.class);
            assertTrue(x.contains("Cannot find public key"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("Expired JWT 🤮")
        @EnumSource(KeyType.class)
        void testExpiredJWT(KeyType keyType) throws NoSuchAlgorithmException {
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add(String.format("%sToken/auth", "here"));
            builder.issuer("macaroon");
            builder.subject("macaroon");
            builder.id(UUID.randomUUID().toString());
            builder.expiration(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .queryParam("client_assertion", jwt)
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED));

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should be unauthorized");
            assertTrue(response.readEntity(String.class).contains("Invalid JWT"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("Incorrect verification key 🤮")
        @EnumSource(KeyType.class)
        void testJWTWrongSigningKey(KeyType keyType) throws NoSuchAlgorithmException {
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);
            final Pair<String, PrivateKey> wrongKeyPair = generateKeypair(keyType);

            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add(String.format("%sToken/auth", "here"));
            builder.issuer("macaroon");
            builder.subject("macaroon");
            builder.id(UUID.randomUUID().toString());
            builder.expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
            builder.signWith(wrongKeyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .queryParam("client_assertion", jwt)
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED));

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should be unauthorized");
            assertTrue(response.readEntity(String.class).contains("Invalid JWT"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT replay attack 🤮")
        @EnumSource(KeyType.class)
        void testJTIReplay(KeyType keyType) throws NoSuchAlgorithmException {
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            String macaroon = buildMacaroon();

            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add(String.format("%sToken/auth", "localhost:3002/v1/"));
            builder.issuer(macaroon);
            builder.subject(macaroon);
            builder.id(UUID.randomUUID().toString());
            builder.expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .queryParam("client_assertion", jwt)
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED));

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Should get a good status on first request");

            // Try to submit again
            Response r2 = RESOURCE.target("/v1/Token/auth")
                    .queryParam("scope", "system/*.*")
                    .queryParam("grant_type", "client_credentials")
                    .queryParam("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE)
                    .queryParam("client_assertion", jwt)
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED));

            assertAll(() -> assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), r2.getStatus(), "Should be unauthorized"),
                    () -> assertTrue(r2.readEntity(String.class).contains("Invalid JWT"), "Should have invalid JWT"));
        }
    }

    @Nested
    @DisplayName("JWT Validation Tests")
    class ValidationTests {

        @ParameterizedTest
        @DisplayName("Invalid JWT 🤮")
        @EnumSource(KeyType.class)
        void testNonToken(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add("localhost:3002/v1/Token/auth");
            builder.issuer("macaroon");
            builder.subject("macaroon");
            builder.id(UUID.randomUUID().toString());
            builder.expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("JWT is not formatted, signed, or populated correctly"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("Invalid JWT with random UUID 🤮")
        @EnumSource(KeyType.class)
        void testUUIDToken(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final String id = UUID.randomUUID().toString();
 
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add("localhost:3002/v1/Token/auth");
            builder.issuer(id);
            builder.subject(id);
            builder.id(id);
            builder.expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("JWT is not formatted, signed, or populated correctly"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("Expired JWT 🤮")
        @EnumSource(KeyType.class)
        void testExpiredJWT(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final String id = UUID.randomUUID().toString();

            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getKey());
            builder.audience().add(String.format("%sToken/auth", "here"));
            builder.issuer(id);
            builder.subject(id);
            builder.id(id);
            builder.expiration(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("JWT is expired"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT with numeric expiration 🤮")
        @EnumSource(KeyType.class)
        void testNumericExpiration(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final Map<String, Object> claims = new HashMap<>();
            claims.put("exp", -100);

            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add(String.format("%sToken/auth", "here"));
            builder.issuer(id);
            builder.subject(id);
            builder.id(id);
            builder.claims().add(claims);
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("JWT is expired"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT with missing expiration date 🤮")
        @EnumSource(KeyType.class)
        void testNoDateExpiration(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final Map<String, Object> claims = new HashMap<>();
            claims.put("exp", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atOffset(ZoneOffset.UTC).minus(1, ChronoUnit.CENTURIES)));

            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add(String.format("%sToken/auth", "here"));
            builder.issuer(id);
            builder.subject(id);
            builder.id(id);
            builder.claims().add(claims);
            builder.signWith(keyPair.getRight());
            builder.expiration(null);

            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("Expiration time must be seconds since unix epoch"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT eclipses size limit 🤮")
        @EnumSource(KeyType.class)
        void testOverlongJWT(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add(String.format("%sToken/auth", "here"));
            builder.issuer(id);
            builder.subject(id);
            builder.id(id);
            builder.expiration(Date.from(Instant.now().plus(12, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("Token expiration cannot be more than 5 minutes in the future"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT with incorrect audit claim 🤮")
        @EnumSource(KeyType.class)
        void testWrongAudClaim(KeyType keyType) throws NoSuchAlgorithmException {
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);
            final String m = buildMacaroon();

            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add(String.format("%sToken/auth", "here"));
            builder.issuer(m);
            builder.subject(m);
            builder.id(id);
            builder.expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("Audience claim value is incorrect"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("Successful JWT verification 🥳")
        @EnumSource(KeyType.class)
        void testSuccess(KeyType keyType) throws NoSuchAlgorithmException {
            final String m = buildMacaroon();
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add("localhost:3002/v1/Token/auth");
            builder.issuer(m);
            builder.subject(m);
            builder.id(id);
            builder.expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Should be valid");
        }

        @ParameterizedTest
        @DisplayName("JWT with mismatched claims 🤮")
        @EnumSource(KeyType.class)
        void testMismatchClaims(KeyType keyType) throws NoSuchAlgorithmException {
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add(String.format("%sToken/auth", "here"));
            builder.issuer("this is");
            builder.subject("not matching");
            builder.id(id);
            builder.expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("JWT is not formatted, signed, or populated correctly"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT with missing claims 🤮")
        @EnumSource(KeyType.class)
        void testMissingClaim(KeyType keyType) throws NoSuchAlgorithmException {
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add(String.format("%sToken/auth", "here"));
            builder.subject("not matching");
            builder.id(id);
            builder.expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("Claim `issuer` must be present"), "Should have correct exception");
        }

        @Test
        @DisplayName("Incorrectly-formatted JWT 🤮")
        void testNotJWT() {
            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity("this is not a jwt", MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("JWT is not formatted, signed, or populated correctly"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT with missing verification key id 🤮")
        @EnumSource(KeyType.class)
        void testMissingKID(KeyType keyType) throws NoSuchAlgorithmException {
            final String m = buildMacaroon();
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);

            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.audience().add("localhost:3002/v1/Token/auth");
            builder.issuer(m);
            builder.subject(m);
            builder.id(id);
            builder.expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getPrivate());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("JWT header must have `kid` value"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT with invalid verification key id 🤮")
        @EnumSource(KeyType.class)
        void testInvalidKID(KeyType keyType) throws NoSuchAlgorithmException {
            final String m = buildMacaroon();
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);

            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.audience().add("localhost:3002/v1/Token/auth");
            builder.header().add("kid", "this is not a kid");
            builder.issuer(m);
            builder.subject(m);
            builder.id(id);
            builder.expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getPrivate());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("`kid` value must be a UUID"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT with incorrect expiration date format 🤮")
        @EnumSource(KeyType.class)
        void testIncorrectExpFormat(KeyType keyType) throws NoSuchAlgorithmException {
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);
            final String m = buildMacaroon();

            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add("localhost:3002/v1/Token/auth");
            builder.issuer(m);
            builder.subject(m);
            builder.id(id);
            builder.claim("exp", Instant.now().plus(1, ChronoUnit.MINUTES).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            builder.signWith(keyPair.getRight());
            
            final String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6ImY1OTAxNjUxNTllOTljZTc4YzY3OGNkNGI0OTY1NjE5In0.eyJpc3MiOiJodHRwczovL2lkcC5sb2NhbCIsImF1ZCI6Im15X2NsaWVudF9hcHAiLCJzdWIiOiI1YmU4NjM1OTA3M2M0MzRiYWQyZGEzOTMyMjIyZGFiZSIsImV4cCI6IkNodWNrIGlzIHByZXR0eSBnb29kISIsImlhdCI6MTcyODg4MTUzMH0.ZzQQDC-aC2TF_FE3R93fGFnUg5buvmB1wrvy9zqplpRDwzMHE5C1rCBr8ozkg8EXlvJc6_81ck3Av3yBqtFZ6Hm_mfAn_B-cyuhTTPNIxLEZI8VlDvJ5EU2SaU6hWy1pFSHh3nvt2shVuNZjnw3ggPpHfHVwm2qMwW1Jg7k3lNCD__2pwxVzH2nZGrG2qLPje32mQy2l8TeEi1WfQo8z9BX-6_XEepDvV2zCVSbcRTvbtxP93mL3nA2Y76FThA4dA7J2XXdVYR5CWH-Coo0BWWvAK-cnbCUtH41km_zEW3OUjBwIMmqZoxXLSb4iRxaOLsIWtFk9ZOPvvcPQRNKebuFrLKLVQA6uzll5qeghCIdqRwg9YQhlHTiTkD5Jgye45T1vDMDHR8SFY8P1QukIrQBnpC0Rh1JHylW1PtFy4kJ1vJzP9O7bYS1AYOuGfL2UVHtqKJ-N1Vi8yPUUK-GYOdXxrZnY6zcEXZx8LaR0PTqk9vbfYJw7bMv-bzlgM9n081IMHWdkWyPsZOarIbBZme0ld4meMrOkwKXzOgZsdxBrNXcsYbcVcp2mEWzn4m9cLtSV9p2v--OujWxeOY0Bfe11NBiu07Sb2dydyx-hFAJBmbzjrAPXK0d4DP1_ifjnlS5IArSArRRd4xyyqF3xgbyfZVZD89zovXEgOgqqDjg";

            // Submit the JWT
                Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("JWT is not formatted, signed, or populated correctly"), "Should have correct exception");
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

        final JwtKeyResolver resolver = spy(new JwtKeyResolver(publicKeyDAO));
        final CaffeineJTICache jtiCache = new CaffeineJTICache();

        UUID organizationID = UUID.randomUUID();
        doReturn(organizationID).when(resolver).getOrganizationID(Mockito.anyString());

        final TokenPolicy tokenPolicy = new TokenPolicy();

        final DPCAuthFactory factory = new DPCAuthFactory(bakery, new MacaroonsAuthenticator(client), tokenDAO, dpc401handler);
        final DPCAuthDynamicFeature dynamicFeature = new DPCAuthDynamicFeature(factory);

        final TokenResource tokenResource = new TokenResource(tokenDAO, publicKeyDAO, bakery, tokenPolicy, resolver, jtiCache, "localhost:3002/v1");
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
            @SuppressWarnings("RedundantCast") final KeyPair keyPair = JWTKeys.get((UUID) answer.getArgument(1));
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
