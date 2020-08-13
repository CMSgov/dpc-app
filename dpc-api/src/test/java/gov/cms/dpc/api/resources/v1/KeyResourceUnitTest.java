package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeyResourceUnitTest {

    @Mock
    PublicKeyDAO publicKeyDao;

    KeyResource resource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        resource = new KeyResource(publicKeyDao);
    }

    @Test
    public void testSubmitKey() throws GeneralSecurityException, IOException {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        KeyResource.KeySignature keySignature = KeyResourceTest.generateKeyAndSignature();

        String label = "A test key label";

        resource.submitKey(organizationPrincipal, keySignature, Optional.of(label));

        ArgumentCaptor<PublicKeyEntity> keyEntityArgumentCaptor = ArgumentCaptor.forClass(PublicKeyEntity.class);
        Mockito.verify(publicKeyDao).persistPublicKey(keyEntityArgumentCaptor.capture());

        PublicKeyEntity keyEntity = keyEntityArgumentCaptor.getValue();
        assertEquals(orgId, keyEntity.getOrganization_id());
        assertEquals(label, keyEntity.getLabel());
        assertTrue(keySignature.getKey().replaceAll("[\n\r]+", "").contains(
                Base64.getMimeEncoder().encodeToString(
                        keyEntity.getPublicKey().parsePublicKey().getEncoded()
                ).replaceAll("[\n\r]+", "")));
    }
}
