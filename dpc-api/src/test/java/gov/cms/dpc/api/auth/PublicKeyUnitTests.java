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
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
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
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Unit tests for public key handling")
@SuppressWarnings("InnerClassMayBeStatic")
class PublicKeyUnitTests {

    private static final ResourceExtension RESOURCE = buildResources();
    private static final Map<UUID, PublicKeyEntity> PUBLIC_KEYS = new HashMap<>();

    private PublicKeyUnitTests() {
    }

    @Nested
    @DisplayName("Key submission tests")
    class KeySubmissionTests {

        @Test
        void testMediaType() throws NoSuchAlgorithmException {
            final KeyPair key = APITestHelpers.generateKeyPair();

            final Response response = RESOURCE
                    .target("/Key")
                    .request()
                    .post(Entity.entity(APITestHelpers.generatePublicKey(key.getPublic()), MediaType.APPLICATION_JSON));

            assertEquals(415, response.getStatus(), "Should not support FHIR JSON");
        }

        @Test
        void testInvalidKey() {
            final Response response = RESOURCE
                    .target("/Key")
                    .request()
                    .post(Entity.entity("APITestHelpers.generatePublicKey(key.getPublic())", MediaType.TEXT_PLAIN));

            assertAll(() -> assertEquals(400, response.getStatus(), "Should not accept nonsense data"),
                    () -> assertTrue(response.readEntity(String.class).contains("Public key is not valid"), "Should have correct error message"));
        }

        @Test
        void testKeyDefaultLabel() throws NoSuchAlgorithmException {
            final KeyPair key = APITestHelpers.generateKeyPair();
            final Response response = RESOURCE
                    .target("/Key")
                    .request()
                    .post(Entity.entity(APITestHelpers.generatePublicKey(key.getPublic()), MediaType.TEXT_PLAIN));

            assertAll(() -> assertEquals(200, response.getStatus(), "Should have succeeded"),
                    () -> assertTrue(response.readEntity(KeyView.class).label.startsWith("key:"), "Should have default label"));
        }

        @Test
        void testKeyCustomLabel() throws NoSuchAlgorithmException {
            final String label = "This is a label";
            final KeyPair key = APITestHelpers.generateKeyPair();
            final Response response = RESOURCE
                    .target("/Key")
                    .queryParam("label", label)
                    .request()
                    .post(Entity.entity(APITestHelpers.generatePublicKey(key.getPublic()), MediaType.TEXT_PLAIN));

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
