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

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Date;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
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
        @DisplayName("JWT with invalid payload 🤮")
        void testFormParams() {
            final String payload = "this is not a payload";
            final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
            formData.add("scope", "");
            formData.add("grant_type", "");
            formData.add("client_assertion_type", "");
            formData.add("client_assertion", "");

            Response response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should have all exceptions
            DPCValidationErrorMessage validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have failed");
            assertEquals(4, validationErrorResponse.getErrors().size(), "Should have four violations");

            formData.putSingle("scope", "system/*.*");
            // Add the missing scope value and try again
            response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should have one less exception
            validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have failed");
            assertEquals(3, validationErrorResponse.getErrors().size(), "Should have three violations");

            formData.putSingle("grant_type", "client_credentials");


            // Add the grant type
            response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should still have an exception
            validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have failed");
            assertEquals(2, validationErrorResponse.getErrors().size(), "Should have two violation");

            // Add the assertion type
            formData.putSingle("client_assertion_type", TokenResource.CLIENT_ASSERTION_TYPE);
            response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should another for the empty client_assertion
            validationErrorResponse = response.readEntity(DPCValidationErrorMessage.class);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should have a 400 from an empty param");
            assertEquals(1, validationErrorResponse.getErrors().size(), "Should only have a single violation");
            assertTrue(validationErrorResponse.getErrors().get(0).contains("Assertion is required"));

            // Add the token and try again
            formData.putSingle("client_assertion", payload);

            response = RESOURCE.target("/v1/Token/auth")
                    .request()
                    .post(Entity.form(formData));

            // Should have no validation exceptions, but still fail
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should be a bad request (invalid payload)");
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
    /*
        Relevant tests involve the location and return of the key for verifying a signed JWT.
        This is limited to reading the JWT header and retrieving the key using the KeyID.
        Verification of the signature using that key and not whether the internal claims or 
        headers are conformant/correct with our application:
    
        No KID submitted                testMissingKIDField                 Unauthorized
        Invalid KID (non-UUID)          testNonUUIDKeyID                    Bad request
        KID not found in DB             testMissingSigningKey               Unauthorized
        KID returns an invalid key      testFailingKeyParsing               Internal server error
        KID found                       testRSASigningKeyResolver           OK
                                        testECCSigningKeyResolver  
    */
    
        @ParameterizedTest
        @DisplayName("JWT public key not found in key store 🤮")
        @EnumSource(KeyType.class)
        void testMissingJWTPublicKey(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with missing key
            final String m = buildMacaroon();
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);

            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", UUID.randomUUID().toString());
            builder.audience().add("localhost:3002/v1/Token/auth");
            builder.issuer(m);
            builder.subject(m);
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
            assertTrue(response.readEntity(String.class).contains("Cannot find public key"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("Expired JWT 🤮")
        @EnumSource(KeyType.class)
        void testExpiredJWT(KeyType keyType) throws NoSuchAlgorithmException {
            final String m = buildMacaroon();
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add("localhost:3002/v1/Token/auth");
            builder.issuer(m);
            builder.subject(m);
            builder.id(id);
            builder.expiration(Date.from(Instant.now().minus(6, ChronoUnit.MINUTES)));
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
            assertTrue(response.readEntity(String.class).contains("JWT is expired"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("Incorrect verification key 🤮")
        @EnumSource(KeyType.class)
        void testJWTWrongSigningKey(KeyType keyType) throws NoSuchAlgorithmException {
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);
            final Pair<String, PrivateKey> wrongKeyPair = generateKeypair(keyType);
            final String m = buildMacaroon();

            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add("localhost:3002/v1/Token/auth");
            builder.issuer(m);
            builder.subject(m);
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
            assertTrue(response.readEntity(String.class).contains("JWT signature does not match locally-computed signature"), "Should have correct exception");
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
            final String jwt = "this.is_not_a.jwt";

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("JWT is not formatted, signed, or populated correctly"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT with random issuer/subject ID 🤮")
        @EnumSource(KeyType.class)
        void testUUIDToken(KeyType keyType) throws NoSuchAlgorithmException {
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

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should not be valid");
            assertTrue(response.readEntity(String.class).contains("Cannot use Token ID as `client_token`, must use actual token value"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("Expired JWT with date-based expiration 🤮")
        @EnumSource(KeyType.class)
        void testExpiredJWT(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final String m = buildMacaroon();

            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getKey());
            builder.audience().add("localhost:3002/v1/Token/auth");
            builder.issuer(m);
            builder.subject(m);
            builder.id(UUID.randomUUID().toString());
            builder.expiration(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should be unauthorized");
            assertTrue(response.readEntity(String.class).contains("JWT is expired"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("Expired JWT with numeric expiration 🤮")
        @EnumSource(KeyType.class)
        void testNumericExpiration(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);

            final Map<String, Object> claims = new HashMap<>();
            claims.put("exp", -100);

            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add("localhost:3002/v1/Token/auth");
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

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should be unauthorized");
            assertTrue(response.readEntity(String.class).contains("JWT is expired"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT with missing expiration date 🤮")
        @EnumSource(KeyType.class)
        void testNoDateExpiration(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);
            final String m = buildMacaroon();
            final String id = UUID.randomUUID().toString();

            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add("localhost:3002/v1/Token/auth");
            builder.issuer(m);
            builder.subject(m);
            builder.id(id);
            builder.expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            builder.expiration(null);

            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should not be authorized");
            assertTrue(response.readEntity(String.class).contains("Claim expiration must be present"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT lifetime eclipses policy-based limit 🤮")
        @EnumSource(KeyType.class)
        void testOverlongJWT(KeyType keyType) throws NoSuchAlgorithmException {
            // Submit JWT with non-client token
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);
            final String m = buildMacaroon();
            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add("localhost:3002/v1/Token/auth");
            builder.issuer(m);
            builder.subject(m);
            builder.id(id);
            builder.expiration(Date.from(Instant.now().plus(12, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should not be authorized");
            assertTrue(response.readEntity(String.class).contains("Token expiration cannot be more than 5 minutes in the future"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT with incorrect audience claim 🤮")
        @EnumSource(KeyType.class)
        void testWrongAudienceClaim(KeyType keyType) throws NoSuchAlgorithmException {
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

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should not be authorized");
            assertTrue(response.readEntity(String.class).contains("aud claim value is incorrect"), "Should have correct exception");
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
            final String m = buildMacaroon();
            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add("localhost:3002/v1/Token/auth");
            builder.issuer(m);
            builder.subject(m + "🤮");
            builder.id(id);
            builder.expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)));
            builder.signWith(keyPair.getRight());
            
            final String jwt = builder.compact();

            // Submit the JWT
            Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should not be authorized");
            assertTrue(response.readEntity(String.class).contains("Issuer and Subject must be identical"), "Should have correct exception");
        }

        @ParameterizedTest
        @DisplayName("JWT with missing claims 🤮")
        @EnumSource(KeyType.class)
        void testMissingClaim(KeyType keyType) throws NoSuchAlgorithmException {
            final Pair<String, PrivateKey> keyPair = generateKeypair(keyType);
            final String m = buildMacaroon();
            final String id = UUID.randomUUID().toString();
            
            final JwtBuilder builder = Jwts.builder();
            builder.header().add("kid", keyPair.getLeft());
            builder.audience().add("localhost:3002/v1/Token/auth");
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

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Should not be authorized");
            assertTrue(response.readEntity(String.class).contains("Claim issuer must be present"), "Should have correct exception");
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

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should not be authorized");
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
            /*
            
            Built a signed JWT using https://jwt.io/ and https://developer.pingidentity.com/en/tools/jwt-encoder.html
            
            Header:
                {
                    "typ": "JWT",
                    "alg": "RS256",
                    "kid": "f5901651-59e9-9ce7-8c67-8cd4b4965619"
                }
            
            Claims:
                {
                    "iss": {"v": 2, "l": "http://test.local", "i": "0", "c": [{"i64": "b3JnYW5pemF0aW9uX2lkID0gMDc1NTVlYjUtNjhmOS00ZjJjLWJiNDEtNDExZjAxNDE3Yjg0"}], "s64": "5vDEIPQHdtNJ8DdUGVKwQybKT44OydXNheYiiA5w4LE"},
                    "aud": "localhost:3002/v1/Token/auth",
                    "sub": {"v": 2, "l": "http://test.local", "i": "0", "c": [{"i64": "b3JnYW5pemF0aW9uX2lkID0gMDc1NTVlYjUtNjhmOS00ZjJjLWJiNDEtNDExZjAxNDE3Yjg0"}], "s64": "5vDEIPQHdtNJ8DdUGVKwQybKT44OydXNheYiiA5w4LE"},
                    "exp": "Chuck is pretty good!",
                    "jti": "ae35237c-3236-4678-a816-874f0d2b524e"
                }
            
            */
            
            try {
            final String publicKeyPEM = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAldZ+rTLLJ2QQRdsaP5wrU127ZGYS5fygdC1MuVllRlecSJnzXZRuL112Xzj5vWxzr7/ynVptifMI1InsFYdqVLIocq9tcOWTSCMWzl8nazpkm2emWFZrRbky4+AVeK4ArSNwpSGR2yoVo70PkEAr56KQQFhnHtNAvJe6JdU7epc76DtVHr7FUWlSbkxVpBHT/G8CqOV5IJKmHv5aayuykxTVL5L4Um7vdD+gVIOJ8vGWSJi9aqMUUq2PEj7sQbkYdf1GUdqkVNKRlK0LEzaaDMXRvSt9ds9RyqpKcNu+/kDof29+QmrFmmANHIkCJh9UmfJxAJYcj9pSTKeOZYlMSQIDAQAB";
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);
            
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyPEM);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            JWTKeys.put(UUID.fromString("f5901651-59e9-9ce7-8c67-8cd4b4965619"), new KeyPair(publicKey, keyPair.getPrivate()));
            } catch(NoSuchAlgorithmException | InvalidKeySpecException e) {
                // test will fail 
            }
            
            // TEST_ONLY: Invalid JWT for unit testing exp claim format
            // throw-away keys used for pre-signing and test verification
            final String header = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6ImY1OTAxNjUxLTU5ZTktOWNlNy04YzY3LThjZDRiNDk2NTYxOSJ9";
            final String payload = "eyJpc3MiOiJ7XCJ2XCI6MixcImxcIjpcImh0dHA6Ly90ZXN0LmxvY2FsXCIsXCJpXCI6XCIwXCIsXCJjXCI6W3tcImk2NFwiOlwiYjNKbllXNXBlbUYwYVc5dVgybGtJRDBnTURjMU5UVmxZalV0TmpobU9TMDBaakpqTFdKaU5ERXROREV4WmpBeE5ERTNZamcwXCJ9XSxcInM2NFwiOlwiNXZERUlQUUhkdE5KOERkVUdWS3dReWJLVDQ0T3lkWE5oZVlpaUE1dzRMRVwifSIsImF1ZCI6ImxvY2FsaG9zdDozMDAyL3YxL1Rva2VuL2F1dGgiLCJzdWIiOiJ7XCJ2XCI6MixcImxcIjpcImh0dHA6Ly90ZXN0LmxvY2FsXCIsXCJpXCI6XCIwXCIsXCJjXCI6W3tcImk2NFwiOlwiYjNKbllXNXBlbUYwYVc5dVgybGtJRDBnTURjMU5UVmxZalV0TmpobU9TMDBaakpqTFdKaU5ERXROREV4WmpBeE5ERTNZamcwXCJ9XSxcInM2NFwiOlwiNXZERUlQUUhkdE5KOERkVUdWS3dReWJLVDQ0T3lkWE5oZVlpaUE1dzRMRVwifSIsImV4cCI6IkNodWNrIGlzIHByZXR0eSBnb29kISIsImp0aSI6ImFlMzUyMzdjLTMyMzYtNDY3OC1hODE2LTg3NGYwZDJiNTI0ZSJ9";
            final String sig = "BXXbxq0ar-Kr001cPBhr_L9w7AOuEGHaVqXrAH1Nag-t6YjaNNWYpEqw5-DC39cwtrP3r6QBO9VRhqPNbCcru2s9OsF5MANfKmr_oUfnaJLZD6rVgNF5r7PpjqzSjEwsK0e4UVVREk50yrPRFTQ9AB4I4wqftboqOQq5LtVMtsEQ_29NugeadpsD28gH0omySSEqGiNFYMkubgnHNYDCgq3cIGB_0id1VN_Rj8qPeicnsY0MOacXWISqZ2zkDmL5YFcPOPXS19xUCavuzxj4m4F_7MKBDyrKLeXXYFOGqW00cYub0inUhet92STpAyIRDLaI8XRBD1ZVz_nLb1__Lw";
            final String testJwt = String.format("%s.%s.%s", header, payload, sig);

            // Submit the JWT
                Response response = RESOURCE.target("/v1/Token/validate")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(testJwt, MediaType.TEXT_PLAIN));

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should not be authorized");
            assertTrue(response.readEntity(String.class).contains("Expiration is not a JWT NumericDate, nor is it ISO-8601-formatted"), "Should have correct exception");
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
        Mockito.when(tokenDAO.fetchTokens(Mockito.any())).thenAnswer(answer -> "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0");
        final JwtKeyLocator resolver = new JwtKeyLocator(publicKeyDAO);

        final DPCUnauthorizedHandler dpc401handler = mock(DPCUnauthorizedHandler.class);

        final CaffeineJTICache jtiCache = new CaffeineJTICache();

        final TokenPolicy tokenPolicy = new TokenPolicy();

        final DPCAuthFactory factory = new DPCAuthFactory(bakery, new MacaroonsAuthenticator(client), tokenDAO, dpc401handler);
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

        Mockito.when(mock.fetchPublicKey(ArgumentMatchers.any(UUID.class))).then(answer -> {
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
