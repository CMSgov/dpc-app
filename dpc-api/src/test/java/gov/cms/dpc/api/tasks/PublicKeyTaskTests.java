package gov.cms.dpc.api.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMultimap;
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
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
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
    void testKeyUploadNoOrg() throws Exception {
        final ImmutableMultimap<String, String> map = ImmutableMultimap.of();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> upk.execute(map, "", new PrintWriter(new OutputStreamWriter(bos))));
            assertEquals("Must have organization", ex.getMessage(), "Should have correct message");
        }
    }

    @Test
    void testKeyUpload() throws Exception {
        final UUID id = UUID.randomUUID();
        final ImmutableMultimap<String, String> map = ImmutableMultimap.of("organization", id.toString());
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            upk.execute(map, "this is a key", new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(this.keyResource, Mockito.times(1)).submitKey(principalCaptor.capture(), Mockito.eq("this is a key"), Mockito.eq(Optional.empty()));
            assertEquals(id, principalCaptor.getValue().getID(), "Should have correct ID");
        }
    }

    @Test
    void testKeyUploadLabel() throws Exception {
        final UUID id = UUID.randomUUID();
        final ImmutableMultimap<String, String> map = ImmutableMultimap.of("organization", id.toString(), "label", "this is a label");
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            upk.execute(map, "this is a key", new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(this.keyResource, Mockito.times(1)).submitKey(principalCaptor.capture(), Mockito.eq("this is a key"), Mockito.eq(Optional.of("this is a label")));
            assertEquals(id, principalCaptor.getValue().getID(), "Should have correct ID");
        }
    }

    @Test
    void testKeyList() throws Exception {
        final UUID id = UUID.randomUUID();
        final Organization org = new Organization();
        org.setId(id.toString());
        Mockito.when(this.keyResource.getPublicKeys(Mockito.any())).thenAnswer(answer -> {
            assertEquals(id, ((OrganizationPrincipal) answer.getArgument(0)).getID(), "Should have correct ID");
            return new CollectionResponse<PublicKeyEntity>(new ArrayList<>());
        });

        final ImmutableMultimap<String, String> map = ImmutableMultimap.of("organization", id.toString());
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            lpk.execute(map, new PrintWriter(new OutputStreamWriter(bos)));

            @SuppressWarnings("rawtypes") final CollectionResponse response = this.mapper.readValue(new ByteArrayInputStream(bos.toByteArray()), CollectionResponse.class);
            assertTrue(response.getEntities().isEmpty(), "Should have a response, but no members");
        }
    }

    @Test
    void testKeyListNoOrg() throws IOException {
        final ImmutableMultimap<String, String> map = ImmutableMultimap.of();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> lpk.execute(map, new PrintWriter(new OutputStreamWriter(bos))));
            assertEquals("Must have organization", ex.getMessage(), "Should have correct message");
        }
    }

    @Test
    void testKeyDeletionNoOrg() throws IOException {
        final ImmutableMultimap<String, String> map = ImmutableMultimap.of();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> dpk.execute(map, new PrintWriter(new OutputStreamWriter(bos))));
            assertEquals("Must have organization", ex.getMessage(), "Should have correct message");
        }
    }

    @Test
    void testKeyDeletionNoKey() throws IOException {
        final UUID id = UUID.randomUUID();
        final ImmutableMultimap<String, String> map = ImmutableMultimap.of("organization", id.toString());
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> dpk.execute(map, new PrintWriter(new OutputStreamWriter(bos))));
            assertEquals("Must have key", ex.getMessage(), "Should have correct message");
        }
    }

    @Test
    void testKeyDeletion() throws IOException {
        final UUID id = UUID.randomUUID();
        final UUID keyID = UUID.randomUUID();
        final ImmutableMultimap<String, String> map = ImmutableMultimap.of("organization", id.toString(), "key", keyID.toString());
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            dpk.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(keyResource, Mockito.times(1)).deletePublicKey(Mockito.any(), Mockito.eq(keyID));
        }
    }
}
