package gov.cms.dpc.api.auth;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.auth.jwt.JTICache;
import gov.cms.dpc.api.auth.jwt.JwtKeyResolver;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.api.resources.v1.TokenResource;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
class JWTUnitTests {

    private static final ResourceExtension RESOURCE = buildResources();
    private static final Map<String, KeyPair> JWTKeys = new HashMap<>();

    private JWTUnitTests() {
        try {
            JWTKeys.put("correct", APITestHelpers.generateKeyPair());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testQueryParams() {
        final String payload = "this is not a payload";
        final Response response = RESOURCE.target("/Token/auth")
                .request()
                .post(Entity.entity(payload, MediaType.APPLICATION_FORM_URLENCODED));

        final ValidationErrorResponse validationErrorResponse = response.readEntity(ValidationErrorResponse.class);
        assertAll(() -> assertEquals(400, response.getStatus(), "Should have failed"),
                () -> assertEquals(3, validationErrorResponse.errors.size(), "Should have three validations"));
    }

    @Test
    void testJWTHandling() throws NoSuchAlgorithmException {
        // Submit JWT with missing key
        final KeyPair keyPair = APITestHelpers.generateKeyPair();

        final String jwt = Jwts.builder()
                .setHeaderParam("kid", "correct")
                .setAudience(String.format("%sToken/auth", "here"))
                .setIssuer("macaroon")
                .setSubject("macaroon")
                .setId(UUID.randomUUID().toString())
                .setExpiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS384)
                .compact();

        // Submit the JWT
    }

    private static ResourceExtension buildResources() {
        final IGenericClient client = mock(IGenericClient.class);
        final MacaroonBakery bakery = buildBakery();
        final TokenDAO tokenDAO = mock(TokenDAO.class);
        final PublicKeyDAO publicKeyDAO = mock(PublicKeyDAO.class);
        Mockito.when(tokenDAO.fetchTokens(Mockito.any())).thenAnswer(answer -> {
            return "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0";
        });

        final JwtKeyResolver resolver = new JwtKeyResolver(publicKeyDAO);
        final JTICache jtiCache = new JTICache();

        final TokenPolicy tokenPolicy = new TokenPolicy();

        final DPCAuthFactory factory = new DPCAuthFactory(bakery, new MacaroonsAuthenticator(client), tokenDAO);
        final DPCAuthDynamicFeature dynamicFeature = new DPCAuthDynamicFeature(factory);

        final TokenResource tokenResource = new TokenResource(tokenDAO, bakery, tokenPolicy, client, resolver, jtiCache, "localhost:3002/v1");
        final FhirContext ctx = FhirContext.forDstu3();

        return APITestHelpers.buildResourceExtension(ctx, List.of(tokenResource), List.of(dynamicFeature), false);
    }

    private static MacaroonBakery buildBakery() {
        return new MacaroonBakery.MacaroonBakeryBuilder("http://test.local",
                new MemoryRootKeyStore(new SecureRandom()),
                new MemoryThirdPartyKeyStore()).build();
    }

    public static class ValidationErrorResponse {

        public List<String> errors;

        ValidationErrorResponse() {
            // Jackson required
        }
    }
}
