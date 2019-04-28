package gov.cms.dpc.aggregation.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.models.JobModel;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.inject.Inject;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EncryptingAggregationEngine extends AggregationEngine {

    private static final Logger logger = LoggerFactory.getLogger(EncryptingAggregationEngine.class);

    private static final String SYMMETRIC_CIPHER =  "AES/GCM/NoPadding";
    private static final String ASYMMETRIC_CIPHER = "RSA/ECB/PKCS1Padding";
    private static final int KEY_BITS = 128;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_BITS = 96;


    /**
     * Create an engine
     * @param bbclient - the BlueButton client to use
     * @param queue - the Job queue that will direct the work done
     * @param config - the configuration for the engine
     */
    @Inject
    public EncryptingAggregationEngine(BlueButtonClient bbclient, JobQueue queue, Config config) {
        super(bbclient, queue, config);
    }

    @Override
    public String formOutputFilePath(UUID jobID, ResourceType resourceType) {
        return String.format("%s/%s.ndjson.enc", exportPath, JobModel.outputFileName(jobID, resourceType));
    }

    @Override
    protected void workResource(OutputStream writer, JobModel job, ResourceType resourceType) throws Exception { // TODO (isears): Do better
        SecureRandom secureRandom = new SecureRandom();

        // Generate Key
        KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_CIPHER.split("/")[0]);
        keyGenerator.init(KEY_BITS);
        SecretKey secretKey = keyGenerator.generateKey();

        // Generate IV
        byte[] iv =  new byte[IV_BITS / 8];
        secureRandom.nextBytes(iv);


        Cipher aesCipher = Cipher.getInstance(SYMMETRIC_CIPHER);
        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        try(CipherOutputStream cipherOutputStream = new CipherOutputStream(writer, aesCipher);) {
            super.workResource(cipherOutputStream, job, resourceType);
        }

        saveEncryptionMetadata(job, resourceType, secretKey, iv);

        // TODO (isears): Ideally we destroy the key after everything is done, but this throws a DestroyFailedException
        //secretKey.destroy();

    }

    // TODO (isears): re-write this javadoc to reflect changes
    /**
     * Encodes crypto metadata in the following format:
     * {
     *     "SymmetricCipher": "AES cipher type",
     *     "SymmetricKey" : "Symmetric key, encrypted by the asymmetric key (e.g. RSA public key), then base64-encoded",
     *     "SymmetricIv" : "Initialization vector, base64 encoded"
     *     "AsymmetricCipher": "RSA cipher type",
     *     "AsymmetricPublicKey" : "e.g. RSA public key, base64-encoded"
     * }
     *
     * @param job
     * @param resourceType
     * @param aesSecretKey
     * @param iv
     * @throws Exception
     */
    private void saveEncryptionMetadata(JobModel job, ResourceType resourceType, SecretKey aesSecretKey, byte[] iv) throws Exception { // TODO (isears): Do better

        Cipher rsaCipher = Cipher.getInstance(ASYMMETRIC_CIPHER);
        rsaCipher.init(Cipher.ENCRYPT_MODE, job.getRsaPublicKey());

        Map<String,Object> metadata = new HashMap<>();
        Map<String,Object> symmetricMetadata = new HashMap<>();
        Map<String,Object> asymmetricMetadata = new HashMap<>();

        symmetricMetadata.put("Cipher", SYMMETRIC_CIPHER);
        symmetricMetadata.put("EncryptedKey", Base64.getEncoder().encodeToString(rsaCipher.doFinal(aesSecretKey.getEncoded())));
        symmetricMetadata.put("InitializationVector", Base64.getEncoder().encodeToString(iv));
        symmetricMetadata.put("TagLength", GCM_TAG_LENGTH);

        asymmetricMetadata.put("Cipher", ASYMMETRIC_CIPHER);
        asymmetricMetadata.put("PublicKey", Base64.getEncoder().encodeToString(job.getRsaPublicKey().getEncoded()));

        metadata.put("SymmetricProperties", symmetricMetadata);
        metadata.put("AsymmetricProperties", asymmetricMetadata);

        String json = new ObjectMapper().writeValueAsString(metadata);

        try(final FileOutputStream writer = new FileOutputStream(formOutputMetadataPath(job.getJobID(), resourceType))) {
            writer.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String formOutputMetadataPath(UUID jobID, ResourceType resourceType) {
        return String.format("%s/%s-metadata.json", exportPath, JobModel.outputFileName(jobID, resourceType));
    }
}
