package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

public class DefaultBlueButtonClient implements BlueButtonClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultBlueButtonClient.class);

    private String serverBaseUrl;
    private IGenericClient client;

    @Inject
    public DefaultBlueButtonClient(){
        Config conf = ConfigFactory.load();
        String keyStoreType = conf.getString("aggregation.bbclient.keyStore.type");
        String defaultKeyStorePassword = conf.getString("aggregation.bbclient.keyStore.defaultPassword");
        serverBaseUrl = conf.getString("aggregation.bbclient.serverBaseUrl");

        String keyStorePath = System.getProperty("javax.net.ssl.keyStore");
        if(keyStorePath == null || keyStorePath.isEmpty()){
            throw new BlueButtonClientException("javax.net.ssl.keyStore option is empty, cannot find keystore.",
                    new IllegalStateException()
            );
        }

        try (InputStream keyStoreStream = new FileInputStream(keyStorePath)){
            // Need to build a custom HttpClient to handle mutual TLS authentication
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(keyStoreStream, defaultKeyStorePassword.toCharArray());

            SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, defaultKeyStorePassword.toCharArray())
                    .build();

            HttpClient mutualTlsHttpClient = HttpClients.custom().setSSLContext(sslContext).build();
            FhirContext ctx = FhirContext.forDstu3();

            ctx.getRestfulClientFactory().setHttpClient(mutualTlsHttpClient);
            client = ctx.newRestfulGenericClient(this.serverBaseUrl);

        } catch (FileNotFoundException ex){
            throw new BlueButtonClientException("Could not find keystore at location: " + keyStorePath, ex);
        } catch (KeyStoreException ex){
            throw new BlueButtonClientException("Wrong keystore type: " + keyStoreType, ex);
        } catch (IOException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException ex) {
            throw new BlueButtonClientException(
                    "Error reading the keystore: either the default password is wrong or the keystore has been corrupted",
                    ex
            );
        } catch (KeyManagementException ex){
            throw new BlueButtonClientException("Error loading the keystore", ex);
        }
    }

    public Patient requestFHIRFromServer(String beneficiaryID) throws BlueButtonClientException {
        // From http://hapifhir.io/doc_rest_client.html
        Patient patient;

        try {
            patient = client.read().resource(Patient.class).withUrl(buildSearchUrl(beneficiaryID)).execute();

        } catch (ResourceNotFoundException ex) {
            throw new BlueButtonClientException("Could not find beneficiary with ID: " + beneficiaryID, ex);
        }

        return patient;
    }

    private String buildSearchUrl(String patientId){
        return String.format("%sPatient/%s", serverBaseUrl, patientId);
    }

}
