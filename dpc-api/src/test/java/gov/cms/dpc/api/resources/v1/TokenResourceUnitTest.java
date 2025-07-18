package gov.cms.dpc.api.resources.v1;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.jwt.IJTICache;
import gov.cms.dpc.api.auth.jwt.JwtKeyLocator;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.macaroons.config.TokenPolicy.ExpirationPolicy;
import gov.cms.dpc.macaroons.config.TokenPolicy.VersionPolicy;
import io.dropwizard.jersey.jsr310.OffsetDateTimeParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TokenResourceUnitTest {
    @Mock
    TokenDAO mockTokenDao;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private static MacaroonBakery bakery;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private static TokenPolicy policy;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private static Macaroon macaroon;
    private static final JwtKeyLocator locator = Mockito.mock(JwtKeyLocator.class);
    private static final IJTICache cache = Mockito.mock(IJTICache.class);
    private static final String authURL = "auth_url";
    private TokenResource tokenResource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        this.tokenResource = new TokenResource(mockTokenDao, bakery, policy, locator, cache, authURL);
    }

    @Test
    public void testGetOrganizationTokens() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        TokenEntity tokenEntity = new TokenEntity("46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0", orgId, TokenEntity.TokenType.MACAROON);
        List<TokenEntity> tokenEntityList = new ArrayList<>();
        tokenEntityList.add(tokenEntity);
        CollectionResponse<TokenEntity> expected = new CollectionResponse<>(tokenEntityList);

        Mockito.when(mockTokenDao.fetchTokens(orgId)).thenAnswer(answer -> tokenEntityList);

        CollectionResponse<TokenEntity> actualResponse = tokenResource.getOrganizationTokens(organizationPrincipal);

        assertEquals(expected.getEntities(), actualResponse.getEntities());
    }

    @Test
    public void testGetOrganizationToken() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        UUID tokenId = UUID.randomUUID();
        TokenEntity tokenEntity = new TokenEntity(tokenId.toString(), orgId, TokenEntity.TokenType.MACAROON);
        List<TokenEntity> tokenEntityList = new ArrayList<>();
        tokenEntityList.add(tokenEntity);

        Mockito.when(mockTokenDao.findTokenByOrgAndID(orgId, tokenId)).thenAnswer(answer -> tokenEntityList);

        TokenEntity actualResponse = tokenResource.getOrganizationToken(organizationPrincipal, tokenId);

        assertEquals(tokenEntityList.get(0), actualResponse);
    }

    @Test
    public void testGetOrganizationTokenNoMatch() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        UUID tokenId = UUID.randomUUID();

        Mockito.when(mockTokenDao.findTokenByOrgAndID(orgId, tokenId)).thenAnswer(answer -> new ArrayList<TokenEntity>());

        assertThrows(WebApplicationException.class, () -> tokenResource.getOrganizationToken(organizationPrincipal, tokenId));
    }

    @Test
    public void testCreateOrganizationToken() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        ExpirationPolicy expirationPolicy = new ExpirationPolicy();
        expirationPolicy.setExpirationUnit(ChronoUnit.SECONDS);
        expirationPolicy.setExpirationOffset(0);
        VersionPolicy versionPolicy = new VersionPolicy();
        versionPolicy.setCurrentVersion(1);
        List<MacaroonCaveat> macaroonCaveats = new ArrayList<>();
        MacaroonCaveat macaroonCaveat = new MacaroonCaveat();
        macaroonCaveat.setRawCaveat("organization_id = org".getBytes());
        macaroonCaveats.add(macaroonCaveat);
        OffsetDateTime timeNow = OffsetDateTime.now(ZoneOffset.UTC);
        String expires = timeNow.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Optional<OffsetDateTimeParam> optExpires = Optional.of(new OffsetDateTimeParam(expires));
        TokenEntity persistedToken = new TokenEntity(UUID.randomUUID().toString(), orgId, TokenEntity.TokenType.MACAROON);
        String token_result = "final_token";
        persistedToken.setToken(token_result);

        Mockito.when(policy.getExpirationPolicy()).thenReturn(expirationPolicy);
        Mockito.when(policy.getVersionPolicy()).thenReturn(versionPolicy);
        Mockito.when(bakery.createMacaroon(Mockito.any())).thenReturn(macaroon);
        try (MockedStatic<MacaroonBakery> mockedStaticMacaroon = Mockito.mockStatic(MacaroonBakery.class)) {
            mockedStaticMacaroon.when(() -> MacaroonBakery.getCaveats(macaroon)).thenReturn(macaroonCaveats);
            try (MockedStatic<OffsetDateTime> mockedStatic = Mockito.mockStatic(OffsetDateTime.class)) {
                mockedStatic.when(() -> OffsetDateTime.now(ZoneOffset.UTC)).thenReturn(timeNow);
                Mockito.when(mockTokenDao.persistToken(Mockito.any())).thenReturn(persistedToken);
                Mockito.when(bakery.serializeMacaroon(macaroon, true)).thenReturn(token_result.getBytes());

                TokenEntity actualResponse = tokenResource.createOrganizationToken(organizationPrincipal, null, authURL, optExpires);

                assertEquals(persistedToken, actualResponse);
            }
        }
    }

    @Test
    public void testDeleteOrganizationToken() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        UUID tokenId = UUID.randomUUID();
        TokenEntity tokenEntity = new TokenEntity(tokenId.toString(), orgId, TokenEntity.TokenType.MACAROON);
        List<TokenEntity> tokenEntityList = new ArrayList<>();
        tokenEntityList.add(tokenEntity);

        Mockito.when(mockTokenDao.findTokenByOrgAndID(orgId, tokenId)).thenAnswer(answer -> tokenEntityList);
        Mockito.doNothing().when(mockTokenDao).deleteToken(tokenEntityList.get(0));

        Response actualResponse = tokenResource.deleteOrganizationToken(organizationPrincipal, tokenId);

        assertEquals(204, actualResponse.getStatus());
    }

    @Test
    void testAuthorizeJWT_invalidQueryParams() {
        String scope = "system/*.*";
        String grantType = "client_credentials";
        String clientAssertionType = TokenResource.CLIENT_ASSERTION_TYPE;
        String jwtBody = "dummyJWT";

        WebApplicationException scopeExc = assertThrows(WebApplicationException.class, () ->
                tokenResource.authorizeJWT("", grantType, clientAssertionType, jwtBody));
        assertEquals(String.format("Access Scope must be '%s'", scope), scopeExc.getMessage());

        WebApplicationException grantTypeExc = assertThrows(WebApplicationException.class, () ->
                tokenResource.authorizeJWT(scope, "", clientAssertionType, jwtBody));
        assertEquals(String.format("Grant Type must be '%s'", grantType), grantTypeExc.getMessage());

        WebApplicationException clientAssertionExc = assertThrows(WebApplicationException.class, () ->
                tokenResource.authorizeJWT(scope, grantType, "", jwtBody));
        assertEquals(String.format("Client Assertion Type must be '%s'", clientAssertionType), clientAssertionExc.getMessage());

        WebApplicationException jwtBodyExc = assertThrows(WebApplicationException.class, () ->
                tokenResource.authorizeJWT(scope, grantType, clientAssertionType, ""));
        assertEquals("Client Assertion must be present", jwtBodyExc.getMessage());
    }
}
