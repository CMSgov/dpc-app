package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.aggregation.DPCAggregationConfiguration;
import io.github.resilience4j.retry.RetryConfig;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.MissingResourceException;

public class BlueButtonClientModule extends DropwizardAwareModule<DPCAggregationConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(BlueButtonClientModule.class);
    // Used to retrieve the keystore from the JAR resources. This path is relative to the Resources root.
    private static final String KEYSTORE_RESOURCE_KEY = "/bb.keystore";
    private BBClientConfiguration bbClientConfiguration;

    public BlueButtonClientModule() {
        // Not used
    }

    public BlueButtonClientModule(BBClientConfiguration config) {
        this.bbClientConfiguration = config;
    }

    @Override
    public void configure(Binder binder) {
        // If the config is null, pull it from Dropwizard
        // This is gross, but necessary in order to get the injection to be handled correctly in both prod/test
        if (this.bbClientConfiguration == null) {
            this.bbClientConfiguration = getConfiguration().getClientConfiguration();
        }
    }

    @Provides
    public BlueButtonClient provideBlueButtonClient(IGenericClient fhirRestClient) {
        return new DefaultBlueButtonClient(fhirRestClient);
    }

    @Provides
    public IGenericClient provideFhirRestClient(FhirContext fhirContext, HttpClient httpClient) {
        fhirContext.getRestfulClientFactory().setHttpClient(httpClient);

        return fhirContext.newRestfulGenericClient(this.bbClientConfiguration.getServerBaseUrl());
    }

    @Provides
    public KeyStore provideKeyStore() {

        final BBClientConfiguration.KeystoreConfiguration keystoreConfiguration = this.bbClientConfiguration.getKeystore();

        try (final InputStream keyStoreStream = getKeyStoreStream()) {
            KeyStore keyStore = KeyStore.getInstance(keystoreConfiguration.getType());
            keyStore.load(keyStoreStream, keystoreConfiguration.getDefaultPassword().toCharArray());
            return keyStore;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            logger.error(ex.getMessage());
            throw new BlueButtonClientSetupException(ex.getMessage(), ex);
        }
    }

    @Provides
    public HttpClient provideHttpClient(KeyStore keyStore) {
        return buildMutualTlsClient(keyStore, this.bbClientConfiguration.getKeystore().getDefaultPassword().toCharArray());
    }

    @Provides
    RetryConfig provideRetryConfig() {
        // Create retry handler with our custom defaults
        return RetryConfig.custom()
                .maxAttempts(this.bbClientConfiguration.getRetryCount())
                .build();
    }

    /**
     * Helper function get the keystore from either the location specified in the Configuration file, or from the JAR resources.
     * If the Config path is set, the helper will try to pull from the absolute file path.
     * Otherwise it looks for the {@link BlueButtonClientModule#KEYSTORE_RESOURCE_KEY} in the resources path.
     *
     * @return - {@link InputStream} to keystore
     */
    private InputStream getKeyStoreStream() {
        final InputStream keyStoreStream;

        if (this.bbClientConfiguration.getKeystore().getLocation() == null) {
            keyStoreStream = DefaultBlueButtonClient.class.getResourceAsStream(KEYSTORE_RESOURCE_KEY);
            if (keyStoreStream == null) {
                logger.error("KeyStore location is empty, cannot find keyStore {} in resources", KEYSTORE_RESOURCE_KEY);
                throw new BlueButtonClientSetupException("Unable to get keystore from resources",
                        new MissingResourceException("", DefaultBlueButtonClient.class.getName(), KEYSTORE_RESOURCE_KEY));
            }
        } else {
            final String keyStorePath = this.bbClientConfiguration.getKeystore().getLocation();
            logger.debug("Opening keystream from location: {}", keyStorePath);
            try {
                keyStoreStream = new FileInputStream(keyStorePath);
            } catch (FileNotFoundException e) {
                logger.error("Could not find keystore at location: {}" + Paths.get(keyStorePath).toAbsolutePath().toString());
                throw new BlueButtonClientSetupException("Unable to find keystore", e);
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
        final SSLContext sslContext;

        try {
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, keyStorePass)
                    .loadTrustMaterial(keyStore, null)
                    .build();

        } catch (KeyManagementException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException ex) {
            logger.error(ex.getMessage());
            throw new BlueButtonClientSetupException(ex.getMessage(), ex);
        }

        // Configure the socket timeout for the connection, incl. ssl tunneling
        final BBClientConfiguration.TimeoutConfiguration timeouts = this.bbClientConfiguration.getTimeouts();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeouts.getConnectionTimeout())
                .setConnectionRequestTimeout(timeouts.getRequestTimeout())
                .setSocketTimeout(timeouts.getSocketTimeout())
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setSSLContext(sslContext)
                .build();
    }
}
