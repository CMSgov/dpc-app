package gov.cms.dpc.bluebutton;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheHttp5RestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.bluebutton.client.BlueButtonClientImpl;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.bluebutton.config.BBClientConfiguration;
import gov.cms.dpc.bluebutton.config.BlueButtonBundleConfiguration;
import gov.cms.dpc.bluebutton.exceptions.BlueButtonClientSetupException;
import gov.cms.dpc.bluebutton.health.BlueButtonHealthCheck;
import gov.cms.dpc.fhir.configuration.TimeoutConfiguration;
import io.dropwizard.core.Configuration;
import jakarta.inject.Named;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

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
 * Guice module for building and injecting the {@link BlueButtonClient}.
 *
 * @param <T> - Dropwizard {@link Configuration} class that implements {@link BlueButtonBundleConfiguration}
 */
public class BlueButtonClientModule<T extends Configuration & BlueButtonBundleConfiguration> extends DropwizardAwareModule<T> {

    private static final Logger logger = LoggerFactory.getLogger(BlueButtonClientModule.class);
    // Used to retrieve the keystore from the JAR resources. This path is relative to the Resources root.
    private static final String KEYSTORE_RESOURCE_KEY = "/bb.keystore";
    private BBClientConfiguration bbClientConfiguration;

    public BlueButtonClientModule() {
        this.bbClientConfiguration = null;
    }

    public BlueButtonClientModule(BBClientConfiguration config) {
        this.bbClientConfiguration = config;
    }

    @Override
    public void configure() {
        Binder binder = binder();

        if (this.bbClientConfiguration == null) {
            this.bbClientConfiguration = configuration().getBlueButtonConfiguration();
        }

        final boolean healthCheckEnabled = this.bbClientConfiguration.isRegisterHealthCheck();
        if(healthCheckEnabled){
            binder.bind(BlueButtonHealthCheck.class);
        }

        logger.info("Blue Button health checks are {}.", healthCheckEnabled ? "enabled" : "disabled");
    }

    @Provides
    public BlueButtonClient provideBlueButtonClient(@Named("bbclient") IGenericClient fhirRestClient, MetricRegistry registry) {
        return bbClientConfiguration.isUseBfdMock() ? new MockBlueButtonClient(fhirRestClient.getFhirContext()) : new BlueButtonClientImpl(fhirRestClient, this.bbClientConfiguration, registry);
    }

    @Provides
    @Named("bbclient")
    public IGenericClient provideFhirRestClient(FhirContext fhirContext, HttpClient httpClient) {
        fhirContext.setRestfulClientFactory(new ApacheHttp5RestfulClientFactory(fhirContext));
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
            keyStoreStream = BlueButtonClientImpl.class.getResourceAsStream(KEYSTORE_RESOURCE_KEY);
            if (keyStoreStream == null) {
                logger.error("KeyStore location is empty, cannot find keyStore " + KEYSTORE_RESOURCE_KEY + " in resources");
                throw new BlueButtonClientSetupException("Unable to get keystore from resources",
                        new MissingResourceException("", BlueButtonClientImpl.class.getName(), KEYSTORE_RESOURCE_KEY));
            }
        } else {
            final String keyStorePath = this.bbClientConfiguration.getKeystore().getLocation();
            logger.debug("Opening keystream from location: {}", keyStorePath);
            try {
                keyStoreStream = new FileInputStream(keyStorePath);
            } catch (FileNotFoundException e) {
                logger.error("Could not find keystore at location: {}", Paths.get(keyStorePath).toAbsolutePath());
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
            // BlueButton FHIR servers have a self-signed cert and require a client cert
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, keyStorePass)
                    .loadTrustMaterial(keyStore, new TrustSelfSignedStrategy())
                    .build();

        } catch (KeyManagementException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException ex) {
            logger.error(ex.getMessage());
            throw new BlueButtonClientSetupException(ex.getMessage(), ex);
        }

        // Configure the socket timeout for the connection, incl. ssl tunneling
        final TimeoutConfiguration timeouts = this.bbClientConfiguration.getTimeouts();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(timeouts.getConnectionTimeout()))
                .setConnectionRequestTimeout(Timeout.ofSeconds(timeouts.getRequestTimeout()))
                .setResponseTimeout(Timeout.ofSeconds(timeouts.getSocketTimeout()))
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext))
                        .build())
                .build();
    }
}
