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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.MissingResourceException;

public class BlueButtonClientProvider implements Provider<BlueButtonClient> {
    private Config conf;

    private static final Logger logger = LoggerFactory.getLogger(BlueButtonClientProvider.class);
    // Used to retrieve the keystore from the JAR resources. This path is relative to the Resources root.
    private static final String KEYSTORE_RESOURCE_KEY = "/bb.keystore";

    // Error messages
    private static final String MALFORED_URL = "Malformed base URL for bluebutton server";
    private static final String BAD_KEYSTORE = "Error loading key material. It's possible that the keystore password is wrong or the keystore has been corrupted";
    private static final String UNOPENABLE_KEYSTORE = "Could not open keystore";
    private static final String INCOMPATIBLE_KEYSTORE_TYPE = "System was unable to create an instance of of the given keystore type";
    private static final String BAD_CLIENT_CERT_KEY = "There was an issue with the client certificate and/or key";

    @Inject
    public BlueButtonClientProvider(Config conf) {
        this.conf = conf;
    }

    public BlueButtonClient get() {
        String keyStoreType = conf.getString("aggregation.bbclient.keyStore.type");
        String defaultKeyStorePassword = conf.getString("aggregation.bbclient.keyStore.defaultPassword");
        URL serverBaseUrl;
        IGenericClient client;

        try {
            serverBaseUrl = new URL(conf.getString("aggregation.bbclient.serverBaseUrl"));

        } catch (MalformedURLException ex) {
            logger.error(MALFORED_URL, ex);
            throw new BlueButtonClientException(MALFORED_URL, ex);
        }

        try (final InputStream keyStoreStream = getKeyStoreStream()) {
            // Need to build FHIR client capable of doing mutual TLS authentication
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(keyStoreStream, defaultKeyStorePassword.toCharArray());

            FhirContext ctx = FhirContext.forDstu3();

            ctx.getRestfulClientFactory()
                    .setHttpClient(buildMutualTlsClient(keyStore, defaultKeyStorePassword.toCharArray()));
            client = ctx.newRestfulGenericClient(serverBaseUrl.toString());

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
     * Helper function to get the keystore from either the location specified in the Configuration file, or from the JAR resources.
     * If the Config path is set, the helper will try to pull from the absolute file path.
     * Otherwise it looks for the {@link BlueButtonClientProvider#KEYSTORE_RESOURCE_KEY} in the resources path.
     *
     * @return - {@link InputStream} to keystore
     */
    private InputStream getKeyStoreStream() {
        final InputStream keyStoreStream;

        if (!conf.hasPath("aggregation.bbclient.keyStore.location")) {
            keyStoreStream = DefaultBlueButtonClient.class.getResourceAsStream(KEYSTORE_RESOURCE_KEY);
            if (keyStoreStream == null) {
                logger.error("KeyStore location is empty, cannot find keyStore {} in resources", KEYSTORE_RESOURCE_KEY);
                throw new BlueButtonClientException("Unable to get keystore from resources",
                        new MissingResourceException("", DefaultBlueButtonClient.class.getName(), KEYSTORE_RESOURCE_KEY));
            }
        } else {
            final String keyStorePath = conf.getString("aggregation.bbclient.keyStore.location");
            logger.debug("Opening keystream from location: {}", keyStorePath);
            try {
                keyStoreStream = new FileInputStream(keyStorePath);
            } catch (FileNotFoundException e) {
                logger.error("Could not find keystore at location: {}" + Paths.get(keyStorePath).toAbsolutePath().toString());
                throw new BlueButtonClientException("Unable to find keystore", e);
            }
        }
        return keyStoreStream;
    }

    /**
     * Helper function to build a special {@link HttpClient} capable of authenticating with the Blue Button server using a client TLS certificate
     *
     * @param keyStore     {@link KeyStore} containing, at a minimum, the client tls certificate and private key
     * @param keyStorePass password for keystore (default: "changeit")
     * @return {@link HttpClient} compatible with HAPI FHIR TLS client
     */
    private HttpClient buildMutualTlsClient(KeyStore keyStore, char[] keyStorePass) {
        SSLContext sslContext;

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
