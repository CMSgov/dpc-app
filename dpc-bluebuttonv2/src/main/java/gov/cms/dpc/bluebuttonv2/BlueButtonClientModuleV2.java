package gov.cms.dpc.bluebuttonv2;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.bluebuttonv2.client.BlueButtonClientV2;
import gov.cms.dpc.bluebuttonv2.client.BlueButtonClientV2Impl;
import gov.cms.dpc.bluebuttonv2.client.MockBlueButtonClientV2;
import gov.cms.dpc.bluebuttonv2.config.BBClientConfigurationV2;
import gov.cms.dpc.bluebuttonv2.config.BlueButtonBundleConfigurationV2;
import gov.cms.dpc.bluebuttonv2.exceptions.BlueButtonClientSetupExceptionV2;
import io.dropwizard.Configuration;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
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

/**
 * Guice module for building and injecting the {@link BlueButtonClientV2}.
 *
 * @param <T> - Dropwizard {@link Configuration} class that implements {@link BlueButtonBundleConfigurationV2}
 */
public class BlueButtonClientModuleV2<T extends Configuration & BlueButtonBundleConfigurationV2> extends DropwizardAwareModule<T> {

    private static final Logger logger = LoggerFactory.getLogger(BlueButtonClientModuleV2.class);
    // Used to retrieve the keystore from the JAR resources. This path is relative to the Resources root.
    private static final String KEYSTORE_RESOURCE_KEY = "/bb.keystore";
    private BBClientConfigurationV2 bbClientConfigurationV2;

    public BlueButtonClientModuleV2() {
        this.bbClientConfigurationV2 = null;
    }

    public BlueButtonClientModuleV2(BBClientConfigurationV2 config) {
        this.bbClientConfigurationV2 = config;
    }

    @Override
    public void configure(Binder binder) {
        if (this.bbClientConfigurationV2 == null) {
            this.bbClientConfigurationV2 = getConfiguration().getBlueButtonConfigurationV2();
        }
    }

    @Provides
    public BlueButtonClientV2 provideBlueButtonClientR4(@Named("bbclientR4") IGenericClient fhirRestClientV2, MetricRegistry registry) {
        return bbClientConfigurationV2.isUseBfdMock() ? new MockBlueButtonClientV2(fhirRestClientV2.getFhirContext()) : new BlueButtonClientV2Impl(fhirRestClientV2, this.bbClientConfigurationV2, registry);
    }

    @Provides
    @Named("bbclientR4")
    public IGenericClient provideFhirRestClientV2(@Named("fhirContextR4") FhirContext fhirContextR4, HttpClient httpClient) {
        fhirContextR4.getRestfulClientFactory().setHttpClient(httpClient);

        return fhirContextR4.newRestfulGenericClient(this.bbClientConfigurationV2.getServerBaseUrl());
    }

    @Provides
    @Named("fhirContextR4")
    public FhirContext provideFhirContextR4() {
        return FhirContext.forR4();
    }

    @Provides
    public KeyStore provideKeyStore() {

        final BBClientConfigurationV2.KeystoreConfiguration keystoreConfiguration = this.bbClientConfigurationV2.getKeystore();

        try (final InputStream keyStoreStream = getKeyStoreStream()) {
            KeyStore keyStore = KeyStore.getInstance(keystoreConfiguration.getType());
            keyStore.load(keyStoreStream, keystoreConfiguration.getDefaultPassword().toCharArray());
            return keyStore;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            logger.error(ex.getMessage());
            throw new BlueButtonClientSetupExceptionV2(ex.getMessage(), ex);
        }
    }

    @Provides
    public HttpClient provideHttpClient(KeyStore keyStore) {
        return buildMutualTlsClient(keyStore, this.bbClientConfigurationV2.getKeystore().getDefaultPassword().toCharArray());
    }

    /**
     * Helper function get the keystore from either the location specified in the Configuration file, or from the JAR resources.
     * If the Config path is set, the helper will try to pull from the absolute file path.
     * Otherwise it looks for the {@link BlueButtonClientModuleV2#KEYSTORE_RESOURCE_KEY} in the resources path.
     *
     * @return - {@link InputStream} to keystore
     */
    private InputStream getKeyStoreStream() {
        final InputStream keyStoreStream;

        if (this.bbClientConfigurationV2.getKeystore().getLocation() == null) {
            keyStoreStream = BlueButtonClientV2Impl.class.getResourceAsStream(KEYSTORE_RESOURCE_KEY);
            if (keyStoreStream == null) {
                logger.error("KeyStore location is empty, cannot find keyStore {} in resources", KEYSTORE_RESOURCE_KEY);
                throw new BlueButtonClientSetupExceptionV2("Unable to get keystore from resources",
                        new MissingResourceException("", BlueButtonClientV2Impl.class.getName(), KEYSTORE_RESOURCE_KEY));
            }
        } else {
            final String keyStorePath = this.bbClientConfigurationV2.getKeystore().getLocation();
            logger.debug("Opening keystream from location: {}", keyStorePath);
            try {
                keyStoreStream = new FileInputStream(keyStorePath);
            } catch (FileNotFoundException e) {
                logger.error("Could not find keystore at location: {}" + Paths.get(keyStorePath).toAbsolutePath().toString());
                throw new BlueButtonClientSetupExceptionV2("Unable to find keystore", e);
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
            // BlueButton FHIR servers have a self-signed cert and require a client cert
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, keyStorePass)
                    .loadTrustMaterial(keyStore, new TrustSelfSignedStrategy())
                    .build();

        } catch (KeyManagementException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException ex) {
            logger.error(ex.getMessage());
            throw new BlueButtonClientSetupExceptionV2(ex.getMessage(), ex);
        }

        // Configure the socket timeout for the connection, incl. ssl tunneling
        final BBClientConfigurationV2.TimeoutConfiguration timeouts = this.bbClientConfigurationV2.getTimeouts();
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
