package gov.cms.dpc.api.auth.jwt;

import com.github.nitram509.jmacaroons.Macaroon;
import com.github.nitram509.jmacaroons.MacaroonVersion;
import com.github.nitram509.jmacaroons.MacaroonsBuilder;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.KeyType;

import io.jsonwebtoken.JwsHeader;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.mock;

@SuppressWarnings("rawtypes")
@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("JWT verification tests")
class JwtKeyLocatorTests {

    /*
        Relevant tests involve the location and return of the key for verifying a signed JWT.
        This is limited to reading the JWT header and retrieving the key using the KeyID.
        Verification of the signature using that key and not whether the internal claims or 
        headers are conformant/correct with our application:
    
        No KID submitted                testMissingKIDField                 Unauthorized
        Invalid KID (non-UUID)          testNonUUIDKeyID                    Bad request
        KID not found in DB             testMissingSigningKey               Unauthorized
        KID returns an invalid key      testFailingKeyParsing               Internal server error
        KID found                       testRSASigningKeyResolver           OK
                                        testECCSigningKeyResolver  
    */
    
    private PublicKeyDAO dao;
    private JwtKeyLocator resolver;
    private static KeyPair keyPair;
    private static KeyPair eccKeyPair;

    private final static UUID badKeyID = UUID.randomUUID();
    private final static UUID correctKeyID = UUID.randomUUID();
    private final static UUID eccKeyID = UUID.randomUUID();
    private final static UUID notRealKeyID = UUID.randomUUID();

    @BeforeEach
    void setup() throws IOException, NoSuchAlgorithmException {
        keyPair = APIAuthHelpers.generateKeyPair();
        eccKeyPair = APIAuthHelpers.generateKeyPair(KeyType.ECC);
        
        dao = mock(PublicKeyDAO.class);
        resolver = new JwtKeyLocator(dao);

        // Bad entity with malformed key
        final PublicKeyEntity badEntity = mock(PublicKeyEntity.class);
        final SubjectPublicKeyInfo badInfo = mock(SubjectPublicKeyInfo.class);
        Mockito.when(badInfo.getEncoded()).thenReturn("This is not a public key".getBytes());
        Mockito.when(badInfo.getAlgorithm()).thenAnswer(answer -> new AlgorithmIdentifier(new ASN1ObjectIdentifier("1.2.840.113549.1.1"))); // PKCS 1 RSA OID
        Mockito.when(badEntity.getPublicKey()).thenReturn(badInfo);

        // Good entity with real key
        final PublicKeyEntity goodEntity = mock(PublicKeyEntity.class);
        final PublicKeyEntity goodECCEntity = mock(PublicKeyEntity.class);
        Mockito.when(goodECCEntity.getPublicKey()).thenAnswer((answer) -> SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(eccKeyPair.getPublic().getEncoded())));
        Mockito.when(goodEntity.getPublicKey()).thenAnswer((answer) -> SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(keyPair.getPublic().getEncoded())));

        Mockito.when(dao.fetchPublicKey(ArgumentMatchers.any(UUID.class)))
               .thenAnswer(invocation -> {

                   UUID keyId = invocation.getArgument(0);
                   
                   if (keyId.equals(correctKeyID)) return Optional.of(goodEntity);
                   if (keyId.equals(badKeyID)) return Optional.of(badEntity);
                   if (keyId.equals(eccKeyID)) return Optional.of(goodECCEntity);

                   return Optional.empty();
               });        
    }

    @Test
    @DisplayName("RSA verification key retrieval 🥳")
    void testRSASigningKeyResolver() {
        final JwsHeader headerMock = mock(JwsHeader.class);
        Mockito.when(headerMock.getKeyId()).thenReturn(correctKeyID.toString());
        final Key key = resolver.locate(headerMock);

        assertEquals(keyPair.getPublic(), key, "Keys should match");
    }

    @Test
    @DisplayName("ECC verification key retrieval 🥳")
    void testECCSigningKeyResolver() {
        final JwsHeader headerMock = mock(JwsHeader.class);
        Mockito.when(headerMock.getKeyId()).thenReturn(eccKeyID.toString());
        
        final Key key = resolver.locate(headerMock);

        assertEquals(eccKeyPair.getPublic(), key, "Keys should match");
    }

    @Test
    @DisplayName("JWT missing verification key id 🤮")
    void testMissingKIDField() {
        final JwsHeader headerMock = mock(JwsHeader.class);
        Mockito.when(headerMock.getKeyId()).thenReturn(null);

        final NotAuthorizedException exception = assertThrows(NotAuthorizedException.class, () -> resolver.locate(headerMock));

        assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED_401, exception.getResponse().getStatus(), "Should be unauthorized"),
                () -> assertEquals("JWT header must have `kid` value", exception.getMessage(), "Should have KID message"));
    }

    @Test
    @DisplayName("Verification key not found in key store 🤮")
    void testMissingSigningKey() {
        final JwsHeader headerMock = mock(JwsHeader.class);
        Mockito.when(headerMock.getKeyId()).thenReturn(notRealKeyID.toString());

        final NotAuthorizedException exception = assertThrows(NotAuthorizedException.class, () -> resolver.locate(headerMock));

        assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED_401, exception.getResponse().getStatus(), "Should be unauthorized"),
                () -> assertTrue(exception.getMessage().contains("Cannot find public key with id:"), "Should have KID message"));
    }

    @Test
    @Disabled
    @DisplayName("Verification key found in key store is invalid 🤮")
    void testFailingKeyParsing() {
        final JwsHeader headerMock = mock(JwsHeader.class);
//        final Claims mockClaims = mock(Claims.class);
//        Mockito.when(mockClaims.getIssuer()).thenReturn(org1Macaroon);
        Mockito.when(headerMock.getKeyId()).thenReturn(badKeyID.toString());

        final InternalServerErrorException exception = assertThrows(InternalServerErrorException.class, () -> resolver.locate(headerMock));

        assertAll(() -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, exception.getResponse().getStatus(), "Should be unauthorized"),
                () -> assertEquals("Internal server error", exception.getMessage(), "Should have KID message"));
    }

    @Test
    @DisplayName("Malformed verification key id 🤮")
    void testNonUUIDKeyID() {
        final JwsHeader headerMock = mock(JwsHeader.class);
//        final Claims mockClaims = mock(Claims.class);
//        Mockito.when(mockClaims.getIssuer()).thenReturn(org1Macaroon);
        Mockito.when(headerMock.getKeyId()).thenReturn("This is not a real key id");

        final BadRequestException exception = assertThrows(BadRequestException.class, () -> resolver.locate(headerMock));
        
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals("`kid` value must be a UUID", exception.getMessage(), "Should have non-UUID message"));
    }
    
    @Test
    @Disabled
    // this test is no longer in scope as the LocatorAdapter does not validate the signature
    @DisplayName("Retrieved public key doesn't correspond to JWT signing key🤮")
    void testWrongSigningKey() {
        final JwsHeader headerMock = mock(JwsHeader.class);
        Mockito.when(headerMock.getKeyId()).thenReturn(eccKeyID.toString());

        final NotAuthorizedException exception = assertThrows(NotAuthorizedException.class, () -> resolver.locate(headerMock));

        assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED_401, exception.getResponse().getStatus(), "Should be unauthorized"),
                () -> assertTrue(exception.getMessage().contains("Cannot find public key with id:"), "Should have KID message"));
    }    

    @Test
    @DisplayName("No macaroon submitted 🤮")
    @Disabled
    // this test no longer applies to Locator method.  Can be moved to JWT tests
    void testNoMacaroon() {
        final JwsHeader headerMock = mock(JwsHeader.class);
//        final Claims mockClaims = mock(Claims.class);
//        Mockito.when(mockClaims.getIssuer()).thenReturn(null);
        Mockito.when(headerMock.getKeyId()).thenReturn("This is not a real key id");

        final NotAuthorizedException exception = assertThrows(NotAuthorizedException.class, () -> resolver.locate(headerMock));

        assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED_401, exception.getResponse().getStatus(), "Should be unauthorized"),
                () -> assertEquals("JWT must have client_id", exception.getMessage(), "Should have non-UUID message"));
    }

    @Test
    @DisplayName("Macaroon is missing caveat 🤮")
    @Disabled
    // this test no longer applies to Locator method.  Can be moved to JWT tests
    void testMacaroonNoCaveat() {
        final JwsHeader headerMock = mock(JwsHeader.class);
//        final Claims mockClaims = mock(Claims.class);
//        Mockito.when(mockClaims.getIssuer()).thenReturn(makeMacaroon(null));
        Mockito.when(headerMock.getKeyId()).thenReturn("This is not a real key id");

        final NotAuthorizedException exception = assertThrows(NotAuthorizedException.class, () -> resolver.locate(headerMock));

        assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED_401, exception.getResponse().getStatus(), "Should be unauthorized"),
                () -> assertEquals("JWT client token must have organization_id", exception.getMessage(), "Should have non-UUID message"));
    }

    @Test
    @Disabled
    // this test no longer applies to Locator method.  Can be moved to JWT tests
    @DisplayName("Macaroon with wrong org 🤮")
    void testMacaroonWrongOrg() {
        final JwsHeader headerMock = mock(JwsHeader.class);
//        final Claims mockClaims = mock(Claims.class);
//        Mockito.when(mockClaims.getIssuer()).thenReturn(org2Macaroon);
        Mockito.when(headerMock.getKeyId()).thenReturn("This is not a real key id");

        final NotAuthorizedException exception = assertThrows(NotAuthorizedException.class, () -> resolver.locate(headerMock));

        assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED_401, exception.getResponse().getStatus(), "Should be unauthorized"),
                () -> assertEquals("Invalid Public Key ID", exception.getMessage(), "Should have non-UUID message"));
    }

    // since the macaroon is in the claims, its no longer in scope for these tests
    private static String makeMacaroon(UUID orgID) {
        // Manually create a fake Macaroon with just the org id
        final Macaroon m = MacaroonsBuilder.create("test.local", "fake key", "make id");
        if (orgID != null) {
            return MacaroonsBuilder.modify(m)
                    .add_first_party_caveat(String.format("organization_id = %s", orgID.toString()))
                    .getMacaroon()
                    .serialize(MacaroonVersion.SerializationVersion.V1_BINARY);
        }

        return m.serialize(MacaroonVersion.SerializationVersion.V1_BINARY);

    }
}
