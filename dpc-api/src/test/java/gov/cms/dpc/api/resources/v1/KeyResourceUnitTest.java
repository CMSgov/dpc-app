package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Public key resource operations")
public class KeyResourceUnitTest {

    @Mock
    PublicKeyDAO publicKeyDao;

    KeyResource resource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        resource = new KeyResource(publicKeyDao);
    }

    @Test
    @DisplayName("Get public keys from key store 🥳")
public void testGetPublicKeys() {
        OrganizationPrincipal organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();

        PublicKeyEntity publicKeyEntity = mock(PublicKeyEntity.class);
        List<PublicKeyEntity> publicKeyList = List.of(publicKeyEntity);

        when(publicKeyDao.fetchPublicKeys(organizationPrincipal.getID())).thenReturn(publicKeyList);

        Collection<PublicKeyEntity> response = resource.getPublicKeys(organizationPrincipal).getEntities();
        assertEquals(1, response.size());
        assertTrue(response.contains(publicKeyEntity));
    }

    @Test
    @DisplayName("Get public key by ID 🥳")
    public void testGetPublicKey() {
        OrganizationPrincipal organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        UUID publicKeyUUID = UUID.randomUUID();

        PublicKeyEntity publicKeyEntity = mock(PublicKeyEntity.class);
        List<PublicKeyEntity> publicKeyList = List.of(publicKeyEntity);

        when(publicKeyDao.publicKeySearch(publicKeyUUID, organizationPrincipal.getID())).thenReturn(publicKeyList);

        assertEquals(publicKeyEntity, resource.getPublicKey(organizationPrincipal, publicKeyUUID));
    }

    @Test
    @DisplayName("Return unrecognized public key 🤮")
public void testGetPublicKeyNotFound() {
        OrganizationPrincipal organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        UUID publicKeyUUID = UUID.randomUUID();

        when(publicKeyDao.publicKeySearch(publicKeyUUID, organizationPrincipal.getID())).thenReturn(List.of());
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> resource.getPublicKey(organizationPrincipal, publicKeyUUID));
        assertEquals(HttpStatus.SC_NOT_FOUND, exception.getResponse().getStatus());
        assertEquals("Cannot find public key", exception.getMessage());
    }

    @Test
    @DisplayName("Delete public key from key store 🥳")
public void testDeletePublicKey() {
        OrganizationPrincipal organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        UUID publicKeyUUID = UUID.randomUUID();

        PublicKeyEntity publicKeyEntity = mock(PublicKeyEntity.class);
        List<PublicKeyEntity> publicKeyList = List.of(publicKeyEntity);

        when(publicKeyDao.publicKeySearch(publicKeyUUID, organizationPrincipal.getID())).thenReturn(publicKeyList);

        assertEquals(HttpStatus.SC_OK, resource.deletePublicKey(organizationPrincipal, publicKeyUUID).getStatus());
        verify(publicKeyDao).deletePublicKey(publicKeyEntity);
    }

    @Test
    @DisplayName("Delete unrecognized public key 🤮")
public void testDeletePublicKeyNotFound() {
        OrganizationPrincipal organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        UUID publicKeyUUID = UUID.randomUUID();

        when(publicKeyDao.publicKeySearch(publicKeyUUID, organizationPrincipal.getID())).thenReturn(List.of());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> resource.deletePublicKey(organizationPrincipal, publicKeyUUID));
        assertEquals(HttpStatus.SC_NOT_FOUND, exception.getResponse().getStatus());
        assertEquals("Cannot find certificate", exception.getMessage());
    }

    @Test
    @DisplayName("Add public key to key store 🥳")
public void testSubmitKey() throws GeneralSecurityException, IOException {
        OrganizationPrincipal organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        KeyResource.KeySignature keySignature = KeyResourceIT.generateKeyAndSignature();

        String label = "A test key label";

        resource.submitKey(organizationPrincipal, keySignature, Optional.of(label));

        ArgumentCaptor<PublicKeyEntity> keyEntityArgumentCaptor = ArgumentCaptor.forClass(PublicKeyEntity.class);
        Mockito.verify(publicKeyDao).persistPublicKey(keyEntityArgumentCaptor.capture());

        PublicKeyEntity keyEntity = keyEntityArgumentCaptor.getValue();
        assertEquals(organizationPrincipal.getID(), keyEntity.getOrganization_id());
        assertEquals(label, keyEntity.getLabel());
        assertTrue(keySignature.getKey().replaceAll("[\n\r]+", "").contains(
                Base64.getMimeEncoder().encodeToString(
                        keyEntity.getPublicKey().parsePublicKey().getEncoded()
                ).replaceAll("[\n\r]+", "")));
    }

    @Test
    @DisplayName("Add public key to key store with long label 🤮")
public void testSubmitKeyTooLong() throws GeneralSecurityException {
        OrganizationPrincipal organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        KeyResource.KeySignature keySignature = KeyResourceIT.generateKeyAndSignature();
        String label = "A really, really, really long, test key label";

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> resource.submitKey(organizationPrincipal, keySignature, Optional.of(label)));
        assertEquals(HttpStatus.SC_BAD_REQUEST, exception.getResponse().getStatus());
        assertEquals("Key label cannot be more than 25 characters", exception.getMessage());
    }

    @Test
    @DisplayName("Add unreadable public key to key store 🤮")
public void testSubmitKeyBadPEMString() {
        OrganizationPrincipal organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
        KeyResource.KeySignature keySignature = new KeyResource.KeySignature("badPEMString", "badSignature");

        WebApplicationException exception =  assertThrows(WebApplicationException.class,
                () -> resource.submitKey(organizationPrincipal, keySignature, Optional.of("label")));
        assertEquals(HttpStatus.SC_BAD_REQUEST, exception.getResponse().getStatus());
        assertEquals("Public key could not be parsed", exception.getMessage());

    }
}
