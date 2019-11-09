package gov.cms.dpc.api.auth.jwt;

import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.persistence.NoResultException;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(BufferedLoggerHandler.class)
class JwtKeyResolverTests {

    private static JwtKeyResolver resolver;
    private static KeyPair keyPair;

    @BeforeAll
    static void setup() throws IOException, NoSuchAlgorithmException {
        keyPair = APIAuthHelpers.generateKeyPair();
        PublicKeyDAO dao = mock(PublicKeyDAO.class);
        // Bad entity with malformed key
        final PublicKeyEntity badEntity = mock(PublicKeyEntity.class);
        final SubjectPublicKeyInfo badInfo = mock(SubjectPublicKeyInfo.class);
        Mockito.when(badInfo.getEncoded()).thenReturn("This is not a public key".getBytes());
        Mockito.when(badEntity.getPublicKey()).thenReturn(badInfo);

        // Good entity with real key
        final PublicKeyEntity goodEntity = mock(PublicKeyEntity.class);
        final SubjectPublicKeyInfo goodInfo = mock(SubjectPublicKeyInfo.class);
        Mockito.when(goodInfo.getEncoded()).thenReturn(keyPair.getPublic().getEncoded());
        Mockito.when(goodEntity.getPublicKey()).thenReturn(goodInfo);

        Mockito.when(dao.findKeyByLabel("malformed-key")).thenReturn(badEntity);
        Mockito.when(dao.findKeyByLabel("correct-key")).thenReturn(goodEntity);
        Mockito.when(dao.findKeyByLabel("not a real key")).thenThrow(NoResultException.class);
        resolver = new JwtKeyResolver(dao);
    }

    @Test
    void testSigningKeyResolver() {
        final JwsHeader headerMock = mock(JwsHeader.class);
        final Claims mockClaims = mock(Claims.class);
        Mockito.when(headerMock.getKeyId()).thenReturn("correct-key");
        final Key key = resolver.resolveSigningKey(headerMock, mockClaims);

        assertEquals(keyPair.getPublic(), key, "Keys should match");
    }

    @Test
    void testMissingKIDField() {
        final JwsHeader headerMock = mock(JwsHeader.class);
        final Claims mockClaims = mock(Claims.class);
        Mockito.when(headerMock.getKeyId()).thenReturn(null);

        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> resolver.resolveSigningKey(headerMock, mockClaims));

        assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED_401, exception.getResponse().getStatus(), "Should be unauthorized"),
                () -> assertEquals("JWT must have KID field", exception.getMessage(), "Should have KID message"));


    }

    @Test
    void testMissingSigningKey() {
        final JwsHeader headerMock = mock(JwsHeader.class);
        final Claims mockClaims = mock(Claims.class);
        Mockito.when(headerMock.getKeyId()).thenReturn("not a real key");

        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> resolver.resolveSigningKey(headerMock, mockClaims));

        assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED_401, exception.getResponse().getStatus(), "Should be unauthorized"),
                () -> assertTrue(exception.getMessage().contains("Cannot find public key with label"), "Should have KID message"));
    }

    @Test
    void testFailingKeyParsing() {
        final JwsHeader headerMock = mock(JwsHeader.class);
        final Claims mockClaims = mock(Claims.class);
        Mockito.when(headerMock.getKeyId()).thenReturn("malformed-key");

        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> resolver.resolveSigningKey(headerMock, mockClaims));

        assertAll(() -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, exception.getResponse().getStatus(), "Should be unauthorized"),
                () -> assertEquals("Internal server error", exception.getMessage(), "Should have KID message"));
    }
}
