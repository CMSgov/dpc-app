package gov.cms.dpc.api.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.resources.v1.KeyResource;
import gov.cms.dpc.api.tasks.keys.DeletePublicKey;
import gov.cms.dpc.api.tasks.keys.ListPublicKeys;
import gov.cms.dpc.api.tasks.keys.UploadPublicKey;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Public key operations")
public class PublicKeyTaskTests {

    private KeyResource keyResource = Mockito.mock(KeyResource.class);
    private ArgumentCaptor<OrganizationPrincipal> principalCaptor = ArgumentCaptor.forClass(OrganizationPrincipal.class);
    private final UploadPublicKey upk;
    private final ListPublicKeys lpk;
    private final DeletePublicKey dpk;
    private final ObjectMapper mapper;

    PublicKeyTaskTests() {
        this.upk = new UploadPublicKey(keyResource);
        this.dpk = new DeletePublicKey(keyResource);
        this.lpk = new ListPublicKeys(keyResource);
        this.mapper = new ObjectMapper();
    }

    @AfterEach
    void cleanup() {
        Mockito.reset(keyResource);
    }

    @Test
    @DisplayName("Upload public key without specifying an org 🤮")
    void testKeyUploadNoOrg() throws Exception {
        final Map<String, List<String>> map = Map.of();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> upk.execute(map, "", new PrintWriter(new OutputStreamWriter(bos))));
            assertEquals("Must have organization", ex.getMessage(), "Should have correct message");
        }
    }

    @Test
    @DisplayName("Upload public key 🥳")
    void testKeyUpload() throws Exception {
        final UUID id = UUID.randomUUID();
        final Map<String, List<String>> map = Map.of("organization", List.of(id.toString()));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            upk.execute(map, "{\"key\":\"this is a key\",\"signature\":\"this is a signature\"}", new PrintWriter(new OutputStreamWriter(bos)));
            ArgumentCaptor<KeyResource.KeySignature> keySigArg = ArgumentCaptor.forClass(KeyResource.KeySignature.class);
            Mockito.verify(this.keyResource, Mockito.times(1)).submitKey(principalCaptor.capture(),
                    keySigArg.capture(), Mockito.eq(Optional.empty()));
            assertEquals(id, principalCaptor.getValue().getID(), "Should have correct ID");
            assertEquals("this is a key", keySigArg.getValue().getKey());
            assertEquals("this is a signature", keySigArg.getValue().getSignature());
        }
    }

    @Test
    @DisplayName("Upload public key with custom label 🥳")
    void testKeyUploadLabel() throws Exception {
        final UUID id = UUID.randomUUID();
        final Map<String, List<String>> map = Map.of("organization", List.of(id.toString()), "label", List.of("this is a label"));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            upk.execute(map, "{\"key\":\"this is a key\",\"signature\":\"this is a signature\"}", new PrintWriter(new OutputStreamWriter(bos)));
            ArgumentCaptor<KeyResource.KeySignature> keySigArg = ArgumentCaptor.forClass(KeyResource.KeySignature.class);
            Mockito.verify(this.keyResource, Mockito.times(1)).submitKey(principalCaptor.capture(),
                    keySigArg.capture(),
                    Mockito.eq(Optional.of("this is a label")));
            assertEquals(id, principalCaptor.getValue().getID(), "Should have correct ID");
            assertEquals("this is a key", keySigArg.getValue().getKey());
            assertEquals("this is a signature", keySigArg.getValue().getSignature());
        }
    }

    @Test
    @DisplayName("Get list of public keys for org 🥳")
    void testKeyList() throws Exception {
        final UUID id = UUID.randomUUID();
        final Organization org = new Organization();
        org.setId(id.toString());
        Mockito.when(this.keyResource.getPublicKeys(Mockito.any())).thenAnswer(answer -> {
            assertEquals(id, ((OrganizationPrincipal) answer.getArgument(0)).getID(), "Should have correct ID");
            return new CollectionResponse<PublicKeyEntity>(new ArrayList<>());
        });

        final Map<String, List<String>> map = Map.of("organization", List.of(id.toString()));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            lpk.execute(map, new PrintWriter(new OutputStreamWriter(bos)));

            @SuppressWarnings("rawtypes") final CollectionResponse response = this.mapper.readValue(new ByteArrayInputStream(bos.toByteArray()), CollectionResponse.class);
            assertTrue(response.getEntities().isEmpty(), "Should have a response, but no members");
        }
    }

    @Test
    @DisplayName("Get public key list without specifying an org 🤮")
    void testKeyListNoOrg() throws IOException {
        final Map<String, List<String>> map = Map.of();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> lpk.execute(map, new PrintWriter(new OutputStreamWriter(bos))));
            assertEquals("Must have organization", ex.getMessage(), "Should have correct message");
        }
    }

    @Test
    @DisplayName("Delete public key without specifying an org 🤮")
    void testKeyDeletionNoOrg() throws IOException {
        final Map<String, List<String>> map = Map.of();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> dpk.execute(map, new PrintWriter(new OutputStreamWriter(bos))));
            assertEquals("Must have organization", ex.getMessage(), "Should have correct message");
        }
    }

    @Test
    @DisplayName("Delete unrecognized public key 🤮")
    void testKeyDeletionNoKey() throws IOException {
        final UUID id = UUID.randomUUID();
        final Map<String, List<String>> map = Map.of("organization", List.of(id.toString()));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> dpk.execute(map, new PrintWriter(new OutputStreamWriter(bos))));
            assertEquals("Must have key", ex.getMessage(), "Should have correct message");
        }
    }

    @Test
    @DisplayName("Delete public key 🥳")
    void testKeyDeletion() throws IOException {
        final UUID id = UUID.randomUUID();
        final UUID keyID = UUID.randomUUID();
        final Map<String, List<String>> map = Map.of("organization", List.of(id.toString()), "key", List.of(keyID.toString()));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            dpk.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(keyResource, Mockito.times(1)).deletePublicKey(Mockito.any(), Mockito.eq(keyID));
        }
    }
}
