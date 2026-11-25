package gov.cms.dpc.api.auth.staticauth;

import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaticAuthenticatorTest {

    private final StaticAuthenticator authenticator = new StaticAuthenticator();

    // TODO: Remove this test once we get rid of v1
    @Test
    void createsVersionSpecificPrincipals() {
        String organizationId = UUID.randomUUID().toString();

        Principal v1 = authenticator.authenticate(
                new DPCAuthCredentials(null, organizationId, null, null, "v1")).orElseThrow();
        Principal v3 = authenticator.authenticate(
                new DPCAuthCredentials(null, organizationId, null, null, "v3")).orElseThrow();

        assertEquals(organizationId, assertInstanceOf(OrganizationPrincipal.class, v1).getID().toString());
        assertEquals(organizationId, assertInstanceOf(gov.cms.dpc.api.auth.r4.OrganizationPrincipal.class, v3).getID().toString());
    }

    @Test
    void rejectsUnsupportedVersions() {
        DPCAuthCredentials credentials = new DPCAuthCredentials(null, UUID.randomUUID().toString(), null, null, "v2");

        assertThrows(IllegalStateException.class, () -> authenticator.authenticate(credentials));
    }
}
