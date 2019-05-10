package gov.cms.dpc.aggregation.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.models.JobResult;
import io.github.resilience4j.retry.RetryConfig;
import org.hl7.fhir.dstu3.model.ResourceType;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.inject.Inject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EncryptingAggregationEngine extends AggregationEngine {
    
    private String symmetricCipher;
    private String asymmetricCipher;
    private int keyBits;
    private int gcmTagLength;
    private int ivBits;


    /**
     * Create an engine
     * @param bbclient - the BlueButton client to use
     * @param queue - the Job queue that will direct the work done
     * @param config - the configuration for the engine
     */
    @Inject
    public EncryptingAggregationEngine(BlueButtonClient bbclient, JobQueue queue, @ExportPath String exportPath, Config config) {
        super(bbclient, queue, exportPath, RetryConfig.ofDefaults());

        symmetricCipher = config.getString("encryption.symmetricCipher");
        asymmetricCipher = config.getString("encryption.asymmetricCipher");
        keyBits = config.getInt("encryption.keyBits");
        gcmTagLength = config.getInt("encryption.gcmTagLength");
        ivBits = config.getInt("encryption.ivBits");
    }

    @Override
    public String formOutputFilePath(UUID jobID, ResourceType resourceType) {
        return String.format("%s/%s.ndjson.enc", exportPath, JobModel.outputFileName(jobID, resourceType));
    }

    @Override
    public String formErrorFilePath(UUID jobID, ResourceType resourceType) {
        return String.format("%s/%s.ndjson.enc", exportPath, JobModel.errorFileName(jobID, resourceType));
    }

    public String formOutputMetadataPath(UUID jobID, ResourceType resourceType) {
        return String.format("%s/%s-metadata.json", exportPath, JobModel.outputFileName(jobID, resourceType));
    }

    public String formErrorMetadataPath(UUID jobID, ResourceType resourceType) {
        return String.format("%s/%s-metadata.json", exportPath, JobModel.errorFileName(jobID, resourceType));
    }

    /**
     * Creates and configures a {@link CipherOutputStream} and injects it into the Aggregation engine to override the
     * generic FileOutputStream
     *
     * @param writer - the stream to be wrapped in a {@link CipherOutputStream}
     * @param job - the job to process
     * @param jobResult - the per resource-type job results
     */
    @Override
    protected void workResource(OutputStream writer, OutputStream errorWriter, JobModel job, JobResult jobResult) {
        SecureRandom secureRandom = new SecureRandom();

        try {
            // Generate Key
            KeyGenerator keyGenerator = KeyGenerator.getInstance(symmetricCipher.split("/", -1)[0]);
            keyGenerator.init(keyBits);
            SecretKey secretKey = keyGenerator.generateKey();
            SecretKey errorSecretKey = keyGenerator.generateKey();

            // Generate IV
            byte[] iv =  new byte[ivBits / 8];
            secureRandom.nextBytes(iv);
            byte[] errorIV = new byte[ivBits / 8];
            secureRandom.nextBytes(errorIV);

            // Generate Cipher
            Cipher aesCipher = Cipher.getInstance(symmetricCipher);
            aesCipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(gcmTagLength, iv));
            Cipher errorAESCipher = Cipher.getInstance(symmetricCipher);
            errorAESCipher.init(Cipher.ENCRYPT_MODE, errorSecretKey, new GCMParameterSpec(gcmTagLength, errorIV));

            try(CipherOutputStream cipherOutputStream = new CipherOutputStream(writer, aesCipher);
            CipherOutputStream cipherErrorStream = new CipherOutputStream(errorWriter, errorAESCipher)) {
                super.workResource(cipherOutputStream, cipherErrorStream, job, jobResult);
            }

            try(final FileOutputStream metadataWriter = new FileOutputStream(formOutputMetadataPath(job.getJobID(), jobResult.getResourceType()))) {
                saveEncryptionMetadata(metadataWriter, job, jobResult, secretKey, iv);
            }
            if (jobResult.getErrorCount() > 0) {
                try(final FileOutputStream metadataWriter = new FileOutputStream(formErrorMetadataPath(job.getJobID(), jobResult.getResourceType()))) {
                    saveEncryptionMetadata(metadataWriter, job, jobResult, errorSecretKey, errorIV);
                }
            }

            // Ideally, we explicitly remove key material (with secretKey.destroy();) from memory when we're done.
            // Unfortunately, calling secretKey.destroy(); will throw DestroyFailedException
            // As of Apr 2019, there was still no good way to do this in OpenJDK (ref: https://bugs.openjdk.java.net/browse/JDK-8160206)

        } catch(GeneralSecurityException | IOException ex) {
            throw new JobQueueFailure(job.getJobID(), ex);
        }
    }

    /**
     * Encodes crypto metadata in the following format:
     * {
     *     "SymmetricProperties" : {
     *         "Cipher" : (String) AES cipher type (for consumption by javax.crypto.Cipher.getInstance(...)),
     *         "EncryptedKey" : (String) AES secret - encrypted, then base64-encoded,
     *         "InitializationVector" : (String) AES IV - base64-encoded,
     *         "TagLength" : (int) GCM tag length, if using AES/GCM
     *     },
     *
     *     "AsymmetricProperties" : {
     *         "Cipher" : (String) RSA cipher type (for consumption by javax.crypto.Cipher.getInstance(...)),
     *         "PublicKey" : (String) Base64-encoded RSA public key that was initially provided by the vendor
     *     }
     * }
     *
     * This metadata is saved to a json file named [outputFileName]-metadata.json on the tmp filesystem
     *
     * @param job - the current job pulled from the queue
     * @param jobResult - The per resource-type result of the job
     * @param aesSecretKey - the {@link SecretKey} used in the symmetric encryption algorithm to encrypt the data
     * @param iv - a raw byte array corresponding to the iv used by the symmetric encryption algorithm to encrypt the data
     */
    private void saveEncryptionMetadata(OutputStream writer, JobModel job, JobResult jobResult, SecretKey aesSecretKey, byte[] iv) {

        try {

            KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
            Cipher rsaCipher = Cipher.getInstance(asymmetricCipher);
            rsaCipher.init(
                    Cipher.ENCRYPT_MODE,
                    rsaKeyFactory.generatePublic(new X509EncodedKeySpec(job.getRsaPublicKey()))
            );

            Map<String,Object> metadata = new HashMap<>();
            Map<String,Object> symmetricMetadata = new HashMap<>();
            Map<String,Object> asymmetricMetadata = new HashMap<>();

            symmetricMetadata.put("Cipher", symmetricCipher);
            symmetricMetadata.put("EncryptedKey", Base64.getEncoder().encodeToString(rsaCipher.doFinal(aesSecretKey.getEncoded())));
            symmetricMetadata.put("InitializationVector", Base64.getEncoder().encodeToString(iv));
            symmetricMetadata.put("TagLength", gcmTagLength);

            asymmetricMetadata.put("Cipher", asymmetricCipher);
            asymmetricMetadata.put("PublicKey", Base64.getEncoder().encodeToString(job.getRsaPublicKey()));

            metadata.put("SymmetricProperties", symmetricMetadata);
            metadata.put("AsymmetricProperties", asymmetricMetadata);

            String json = new ObjectMapper().writeValueAsString(metadata);
            writer.write(json.getBytes(StandardCharsets.UTF_8));
        } catch(GeneralSecurityException | IOException ex) {
            throw new JobQueueFailure(job.getJobID(), ex);
        }
    }
}
