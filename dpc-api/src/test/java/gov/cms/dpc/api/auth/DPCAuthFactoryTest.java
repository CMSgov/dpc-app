package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.auth.macaroonauth.stu3.MacaroonsAuthenticator;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.macaroons.MacaroonBakery;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DPCAuthFactoryTest {

    // We temporarily have two PathAuthenticators that need to lookup the caller's organization.  The stu3 version that
    // calls attribution to get their stu3 org and the r4 version that gets their r4 org.  This test is just making sure
    // we didn't get our wires crossed.
    // TODO: Remove this once we get rid of v1
    @Test
    void routesPathAuthorizationByRequestVersion() throws Exception {
        MacaroonsAuthenticator v1 = mock(MacaroonsAuthenticator.class);
        gov.cms.dpc.api.auth.macaroonauth.r4.MacaroonsAuthenticator v3 =
                mock(gov.cms.dpc.api.auth.macaroonauth.r4.MacaroonsAuthenticator.class);
        DPCAuthFactory factory = new DPCAuthFactory(mock(MacaroonBakery.class), v1, v3,
                mock(TokenDAO.class), mock(DPCUnauthorizedHandler.class));

        Organization org = new Organization();
        org.setId("Organization/" + UUID.randomUUID());
        PathAuthorizer annotation = mock(PathAuthorizer.class);

        DPCAuthCredentials v1Credentials = new DPCAuthCredentials("token", org, annotation, "id", "v1");
        DPCAuthCredentials v3Credentials = new DPCAuthCredentials("token", org, annotation, "id", "v3");
        OrganizationPrincipal principal = new OrganizationPrincipal(org);
        when(v1.authenticate(v1Credentials)).thenReturn(Optional.of(principal));
        when(v3.authenticate(v3Credentials)).thenReturn(Optional.of(principal));

        assertSame(principal, factory.pathAuthenticator().authenticate(v1Credentials).orElseThrow());
        assertSame(principal, factory.pathAuthenticator().authenticate(v3Credentials).orElseThrow());
        verify(v1).authenticate(v1Credentials);
        verify(v3).authenticate(v3Credentials);

        DPCAuthCredentials unsupported = new DPCAuthCredentials("token", org, annotation, "id", "v2");
        assertThrows(IllegalStateException.class, () -> factory.pathAuthenticator().authenticate(unsupported));
        verifyNoMoreInteractions(v1, v3);
    }
}
