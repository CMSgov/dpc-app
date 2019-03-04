package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;

public class BlueButtonClientProvider implements Provider<BlueButtonClient> {
    private static final Logger logger = LoggerFactory.getLogger(BlueButtonClientProvider.class);

    // Error messages
    private static final String MALFORMED_URL = "Malformed base URL for bluebutton server";
    private static final String BAD_KEYSTORE = "Error loading key material. It's possible that the keystore password is wrong or the keystore has been corrupted";
    private static final String UNOPENABLE_KEYSTORE = "Could not open keystore";
    private static final String INCOMPATIBLE_KEYSTORE_TYPE = "System was unable to create an instance of of the given keystore type";
    private static final String BAD_CLIENT_CERT_KEY = "There was an issue with the client certificate and/or key";

    private Config conf;
    private FhirContext fhirContext;
    private InputStream keyStoreStream;

    @Inject
    public BlueButtonClientProvider(Config conf, FhirContext fhirContext, InputStream keyStoreStream) {
        this.conf = conf;
        this.fhirContext = fhirContext;
        this.keyStoreStream = keyStoreStream;
    }

    public BlueButtonClient get() {
        final String keyStoreType = conf.getString("aggregation.bbclient.keyStore.type");
        final String defaultKeyStorePassword = conf.getString("aggregation.bbclient.keyStore.defaultPassword");
        final URL serverBaseUrl;
        final IGenericClient client;

        try {
            serverBaseUrl = new URL(conf.getString("aggregation.bbclient.serverBaseUrl"));

        } catch (MalformedURLException ex) {
            logger.error(MALFORMED_URL, ex);
            throw new BlueButtonClientException(MALFORMED_URL, ex);
        }

        try (final InputStream keyStoreStream = this.keyStoreStream) {
            // Need to build FHIR client capable of doing mutual TLS authentication
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(keyStoreStream, defaultKeyStorePassword.toCharArray());
            
            fhirContext.getRestfulClientFactory()
                    .setHttpClient(buildMutualTlsClient(keyStore, defaultKeyStorePassword.toCharArray()));
            client = fhirContext.newRestfulGenericClient(serverBaseUrl.toString());

        } catch (IOException ex) {
            throw new BlueButtonClientException(UNOPENABLE_KEYSTORE, ex);
        } catch (KeyStoreException ex) {
            throw new BlueButtonClientException(INCOMPATIBLE_KEYSTORE_TYPE, ex);
        } catch (NoSuchAlgorithmException | CertificateException ex) {
            throw new BlueButtonClientException(BAD_KEYSTORE, ex);
        }

        return new DefaultBlueButtonClient(client, serverBaseUrl);
    }

    /**
     * Helper function to build a special {@link HttpClient} capable of authenticating with the Blue Button server using a client TLS certificate
     *
     * @param keyStore     {@link KeyStore} containing, at a minimum, the client tls certificate and private key
     * @param keyStorePass password for keystore (default: "changeit")
     * @return {@link HttpClient} compatible with HAPI FHIR TLS client
     */
    private HttpClient buildMutualTlsClient(KeyStore keyStore, char[] keyStorePass) {
        final SSLContext sslContext;

        try {
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, keyStorePass)
                    .loadTrustMaterial(keyStore, null)
                    .build();

        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException ex) {
            throw new BlueButtonClientException(BAD_KEYSTORE, ex);
        } catch (KeyManagementException ex) {
            throw new BlueButtonClientException(BAD_CLIENT_CERT_KEY, ex);
        }

        return HttpClients.custom().setSSLContext(sslContext).build();
    }
}
