package gov.cms.dpc.api.auth;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.auth.staticauth.StaticAuthFactory;
import gov.cms.dpc.api.auth.staticauth.StaticAuthenticator;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.resources.v1.KeyResource;
import gov.cms.dpc.common.converters.jackson.StringToOffsetDateTimeConverter;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.KeyType;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Public key handling")
@SuppressWarnings("InnerClassMayBeStatic")
class PublicKeyUnitTests {

    private static final ResourceExtension RESOURCE = buildResources();
    private static final Map<UUID, PublicKeyEntity> PUBLIC_KEYS = new HashMap<>();

    private PublicKeyUnitTests() {
    }

    @Nested
    @DisplayName("Key submission tests")
    class KeySubmissionTests {

        @ParameterizedTest
        @DisplayName("Unsupported media type 🤮")
        @EnumSource(KeyType.class)
        void testMediaType(KeyType keyType) throws NoSuchAlgorithmException {
            final KeyPair key = APIAuthHelpers.generateKeyPair(keyType);

            final Response response = RESOURCE
                    .target("/v1/Key")
                    .request()
                    .post(Entity.entity("{\"key\":\"" + APIAuthHelpers.generatePublicKey(key.getPublic()) + "\",\"signature\":\"\"}", MediaType.TEXT_PLAIN));
            assertEquals(415, response.getStatus(), "Should not support text/plain");
        }

        @Test
        @DisplayName("Invalid key submission 🤮")
        void testInvalidKey() {
            final Response response = RESOURCE
                    .target("/v1/Key")
                    .request()
                    .post(Entity.entity("This is definitely not JSON", MediaType.APPLICATION_JSON));

            assertAll(() -> assertEquals(400, response.getStatus(), "Should not accept nonsense data"),
                    () -> assertTrue(response.readEntity(String.class).contains("Value could not be parsed as JSON"), "Should have correct error message"));
        }

        @ParameterizedTest
        @DisplayName("Default key label 🥳")
        @EnumSource(KeyType.class)
        void testKeyDefaultLabel(KeyType keyType) throws GeneralSecurityException {
            // TODO: Remove. ECC tests skipped temporarily.
            if (KeyType.ECC.equals(keyType)) {
                return;
            }

            final KeyPair key = APIAuthHelpers.generateKeyPair(keyType);
            String sigStr = APIAuthHelpers.signString(key.getPrivate(), KeyResource.SNIPPET);
            final Response response = RESOURCE
                    .target("/v1/Key")
                    .request()
                    .post(Entity.entity(new KeyResource.KeySignature(APIAuthHelpers.generatePublicKey(key.getPublic()), sigStr), MediaType.APPLICATION_JSON));

            assertAll(() -> assertEquals(200, response.getStatus(), "Should have succeeded"),
                    () -> assertTrue(response.readEntity(KeyView.class).label.startsWith("key:"), "Should have default label"));
        }

        @ParameterizedTest
        @DisplayName("Custom key label 🥳")
        @EnumSource(KeyType.class)
        void testKeyCustomLabel(KeyType keyType) throws GeneralSecurityException {
            // TODO: Remove. ECC tests skipped temporarily.
            if (KeyType.ECC.equals(keyType)) {
                return;
            }

            final String label = "This is a label";
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);
            String keyStr = APIAuthHelpers.generatePublicKey(keyPair.getPublic());
            String sigStr = APIAuthHelpers.signString(keyPair.getPrivate(), KeyResource.SNIPPET);
            final Response response = RESOURCE
                    .target("/v1/Key")
                    .queryParam("label", label)
                    .request()
                    .post(Entity.entity(new KeyResource.KeySignature(keyStr, sigStr), MediaType.APPLICATION_JSON));

            assertAll(() -> assertEquals(200, response.getStatus(), "Should have succeeded"),
                    () -> assertEquals(label, response.readEntity(KeyView.class).label, "Should have default label"));
        }
    }

    private static ResourceExtension buildResources() {
        final PublicKeyDAO publicKeyDAO = mockKeyDAO();
        final KeyResource keyResource = new KeyResource(publicKeyDAO);
        final DPCAuthDynamicFeature dpcAuthDynamicFeature = new DPCAuthDynamicFeature(new StaticAuthFactory(new StaticAuthenticator()));
        final FhirContext ctx = FhirContext.forDstu3();

        return APITestHelpers.buildResourceExtension(ctx, List.of(keyResource), List.of(dpcAuthDynamicFeature, new AuthValueFactoryProvider.Binder<>(OrganizationPrincipal.class)), false);
    }

    private static PublicKeyDAO mockKeyDAO() {
        final PublicKeyDAO mock = mock(PublicKeyDAO.class);

        Mockito.when(mock.persistPublicKey(Mockito.any())).then(answer -> {
            final PublicKeyEntity entity = answer.getArgument(0);
            PUBLIC_KEYS.put(entity.getId(), entity);
            return entity;
        });

        return mock;
    }

    @SuppressWarnings("WeakerAccess")
    static class KeyView {

        public UUID id;
        public String publicKey;
        @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
        public OffsetDateTime createdAt;
        public String label;

        KeyView() {
            // Not used
        }
    }
}
