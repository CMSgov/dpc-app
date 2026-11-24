package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.auth.macaroonauth.stu3.MacaroonsAuthenticator;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DPCAuthFactoryTest {

    // Path authorizer returns the correct OrganizationPrincipal (stu3 or r4)
    @Test
    void routesPathAuthorizationByRequestVersion() throws Exception {
        MacaroonsAuthenticator v1 = mock(MacaroonsAuthenticator.class);
        gov.cms.dpc.api.auth.macaroonauth.r4.MacaroonsAuthenticator v3 =
                mock(gov.cms.dpc.api.auth.macaroonauth.r4.MacaroonsAuthenticator.class);
        DPCAuthFactory factory = new DPCAuthFactory(mock(MacaroonBakery.class), v1, v3,
                mock(TokenDAO.class), mock(DPCUnauthorizedHandler.class));

        Organization stu3Org = new Organization();
        stu3Org.setId("Organization/" + UUID.randomUUID());
        PathAuthorizer annotation = mock(PathAuthorizer.class);

        DPCAuthCredentials v1Credentials = new DPCAuthCredentials("token", stu3Org.getIdElement().getIdPart(), annotation, "id", "v1");
        DPCAuthCredentials v3Credentials = new DPCAuthCredentials("token", stu3Org.getIdElement().getIdPart(), annotation, "id", "v3");
        OrganizationPrincipal v1Principal = new OrganizationPrincipal(stu3Org);
        org.hl7.fhir.r4.model.Organization r4Org = new org.hl7.fhir.r4.model.Organization();
        r4Org.setId("Organization/" + stu3Org.getIdElement().getIdPart());
        gov.cms.dpc.api.auth.r4.OrganizationPrincipal v3Principal =
                new gov.cms.dpc.api.auth.r4.OrganizationPrincipal(r4Org);
        when(v1.authenticate(v1Credentials)).thenReturn(Optional.of(v1Principal));
        when(v3.authenticate(v3Credentials)).thenReturn(Optional.of(v3Principal));

        assertSame(v1Principal, factory.versionedAuthenticator().authenticate(v1Credentials).orElseThrow());
        assertSame(v3Principal, factory.versionedAuthenticator().authenticate(v3Credentials).orElseThrow());
        verify(v1).authenticate(v1Credentials);
        verify(v3).authenticate(v3Credentials);

        DPCAuthCredentials unsupported = new DPCAuthCredentials("token", stu3Org.getIdElement().getIdPart(), annotation, "id", "v2");
        Authenticator<DPCAuthCredentials, Principal> authenticator = factory.versionedAuthenticator();
        assertThrows(IllegalStateException.class, () -> authenticator.authenticate(unsupported));
        verifyNoMoreInteractions(v1, v3);
    }

    @Test
    // Standard auth returns the correct OrganizationPrincipal (stu3 or r4)
    void routesStandardAuthenticationByRequestVersion() throws Exception {
        MacaroonsAuthenticator v1 = mock(MacaroonsAuthenticator.class);
        gov.cms.dpc.api.auth.macaroonauth.r4.MacaroonsAuthenticator v3 =
                mock(gov.cms.dpc.api.auth.macaroonauth.r4.MacaroonsAuthenticator.class);
        DPCAuthFactory factory = new DPCAuthFactory(mock(MacaroonBakery.class), v1, v3,
                mock(TokenDAO.class), mock(DPCUnauthorizedHandler.class));
        UUID organizationId = UUID.randomUUID();
        DPCAuthCredentials v1Credentials = new DPCAuthCredentials("token", organizationId.toString(), null, null, "v1");
        DPCAuthCredentials v3Credentials = new DPCAuthCredentials("token", organizationId.toString(), null, null, "v3");
        Organization stu3Org = new Organization();
        stu3Org.setId("Organization/" + organizationId);
        org.hl7.fhir.r4.model.Organization r4Org = new org.hl7.fhir.r4.model.Organization();
        r4Org.setId("Organization/" + organizationId);
        when(v1.authenticate(v1Credentials)).thenReturn(Optional.of(new OrganizationPrincipal(stu3Org)));
        when(v3.authenticate(v3Credentials)).thenReturn(Optional.of(new gov.cms.dpc.api.auth.r4.OrganizationPrincipal(r4Org)));

        assertInstanceOf(OrganizationPrincipal.class, factory.versionedAuthenticator().authenticate(v1Credentials).orElseThrow());
        assertInstanceOf(gov.cms.dpc.api.auth.r4.OrganizationPrincipal.class,
                factory.versionedAuthenticator().authenticate(v3Credentials).orElseThrow());
        verify(v1).authenticate(v1Credentials);
        verify(v3).authenticate(v3Credentials);
    }
}
