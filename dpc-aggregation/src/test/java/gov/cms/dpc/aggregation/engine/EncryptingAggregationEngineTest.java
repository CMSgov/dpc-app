package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import gov.cms.dpc.aggregation.bbclient.MockBlueButtonClient;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.MemoryQueue;
import gov.cms.dpc.queue.models.JobModel;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class EncryptingAggregationEngineTest {
    private static final String TEST_PROVIDER_ID = "1";
    private static final String RSA_PRIVATE_KEY_PATH = "./test_rsa_private_key.der";
    private static final String RSA_PUBLIC_KEY_PATH = "./test_rsa_public_key.der";
    private BlueButtonClient bbclient;
    private JobQueue queue;
    private EncryptingAggregationEngine engine;
    private RSAPublicKey rsaPublicKey;
    private RSAPrivateKey rsaPrivateKey;

    static private Config config;

    @BeforeAll
    static void setupAll() {
        config = ConfigFactory.load("test.application.conf").getConfig("dpc.aggregation");
    }

    @BeforeEach
    void setupEach() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        queue = new MemoryQueue();
        bbclient = new MockBlueButtonClient();
        engine = new EncryptingAggregationEngine(bbclient, queue, config);


        // Ref: https://stackoverflow.com/questions/11410770/load-rsa-public-key-from-file
        KeyFactory keyFactory  = KeyFactory.getInstance("RSA"); // Throws NoSuchAlgorithmException

        Path privateKeyPath = Paths.get(getClass().getClassLoader().getResource(RSA_PRIVATE_KEY_PATH).getFile());
        byte[] privateKeyRaw = Files.readAllBytes(privateKeyPath); // Throws IOException
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyRaw);
        rsaPrivateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec); // Throws InvalidKeySpecException

        Path publicKeyPath = Paths.get(getClass().getClassLoader().getResource(RSA_PUBLIC_KEY_PATH).getFile());
        byte[] publicKeyRaw = Files.readAllBytes(publicKeyPath); // Throws IOException
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyRaw);
        rsaPublicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec); // Throws InvalidKeySpecException
    }

    /**
     * Test if the engine writes encrypted files to the Tmp filesystem
     */
    @Test
    void shouldWriteEncryptedTmpFiles() throws Exception { // TODO (isears): do better
        // Make a simple job with one resource type
        final var jobId = UUID.randomUUID();
        JobModel job = new JobModel(jobId,
                Collections.singletonList(ResourceType.Patient),
                TEST_PROVIDER_ID,
                Collections.singletonList(MockBlueButtonClient.TEST_PATIENT_IDS[0]),
                rsaPublicKey
        );

        // Do the job
        queue.submitJob(jobId, job);
        queue.workJob().ifPresent(pair -> engine.completeJob(pair.getRight()));

        // Look at the result
        assertAll(() -> assertTrue(queue.getJob(jobId).isPresent()),
                () -> assertEquals(JobStatus.COMPLETED, queue.getJob(jobId).get().getStatus()));
        var outputFilePath = engine.formOutputFilePath(jobId, ResourceType.Patient);
        var metadataFilePath = engine.formOutputMetadataPath(jobId, ResourceType.Patient);

        assertTrue(Files.exists(Path.of(outputFilePath)));
        assertTrue(Files.exists(Path.of(metadataFilePath)));

        // Attempt to decrypt result
        String cleartext = decryptTmpFile(Path.of(metadataFilePath), Path.of(outputFilePath));
        IBaseResource result = FhirContext.forDstu3().newJsonParser().parseResource(cleartext);

        assertTrue(result instanceof Patient);
    }

    /**
     * Do a quick encrypt/decrypt test with the test keypair to make sure it's valid
     */
    @Test
    void testKeypairShouldBeValid() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] challenge = new byte[1000];
        ThreadLocalRandom.current().nextBytes(challenge);

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(rsaPrivateKey);
        sig.update(challenge);
        byte[] signature = sig.sign();

        sig.initVerify(rsaPublicKey);
        sig.update(challenge);

        assertTrue(sig.verify(signature));
    }

    private String decryptTmpFile(Path metadataPath, Path dataPath) throws Exception {  // TODO (isears): Do better
        byte[] metadataRaw = Files.readAllBytes(metadataPath);
        Map<String,Object> metadataActual = new ObjectMapper().readValue(metadataRaw, new TypeReference<Map<String,Object>>(){});

        // Check metadata has all required fields
        assertTrue(metadataActual.containsKey("SymmetricProperties"));
        assertTrue(metadataActual.containsKey("AsymmetricProperties"));


        Map symmetricProperties =  (Map) metadataActual.get("SymmetricProperties");
        String symmetricCipher = (String) symmetricProperties.get("Cipher");
        byte[] encryptedSymmetricKey = Base64.getDecoder().decode((String) symmetricProperties.get("EncryptedKey"));
        byte[] symmetricIv = Base64.getDecoder().decode((String) symmetricProperties.get("InitializationVector"));
        int gcmTagLength = (int) symmetricProperties.get("TagLength");

        Map asymmetricProperties = (Map) metadataActual.get("AsymmetricProperties");
        String asymmetricCipher = (String) asymmetricProperties.get("Cipher");

        // Make sure the same RSA public key is echoed back in the metadata
        assertArrayEquals(Base64.getDecoder().decode((String) asymmetricProperties.get("PublicKey")), rsaPublicKey.getEncoded());


        // Initialize asymmetric cipher for decrypting the symmetric key
        Cipher rsaCipher = Cipher.getInstance(asymmetricCipher);
        rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);

        // Initialize the symmetric cipher using the asymmetric cipher to decrypt the secret key
        byte[] aesSecretKeyRaw = rsaCipher.doFinal(encryptedSymmetricKey);
        Cipher aesCipher = Cipher.getInstance(symmetricCipher);
        aesCipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(aesSecretKeyRaw, symmetricCipher.split("/")[0]),
                new GCMParameterSpec(gcmTagLength, symmetricIv)
        );

        try(final FileInputStream reader  = new FileInputStream(dataPath.toString())) {

            CipherInputStream cipherReader = new CipherInputStream(reader, aesCipher);
            return new String(cipherReader.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}