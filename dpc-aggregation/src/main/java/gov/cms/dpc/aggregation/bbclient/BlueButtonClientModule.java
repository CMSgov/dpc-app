package gov.cms.dpc.aggregation.bbclient;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.MissingResourceException;

public class BlueButtonClientModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(BlueButtonClientModule.class);
    // Used to retrieve the keystore from the JAR resources. This path is relative to the Resources root.
    private static final String KEYSTORE_RESOURCE_KEY = "/bb.keystore";

    public BlueButtonClientModule(){

    }

    @Override
    protected void configure() {
        bind(BlueButtonClient.class).toProvider(BlueButtonClientProvider.class);
    }

    /**
     * Provider to get the keystore from either the location specified in the Configuration file, or from the JAR resources.
     * If the Config path is set, the provider will try to pull from the absolute file path.
     * Otherwise it looks for the {@link BlueButtonClientModule#KEYSTORE_RESOURCE_KEY} in the resources path.
     *
     * @return - {@link InputStream} to keystore
     */
    // TODO: build keystore with access to config here
    @Provides
    public InputStream provideKeyStore(Config config) {
        final InputStream keyStoreStream;

        if (!config.hasPath("aggregation.bbclient.keyStore.location")) {
            keyStoreStream = DefaultBlueButtonClient.class.getResourceAsStream(KEYSTORE_RESOURCE_KEY);
            if (keyStoreStream == null) {
                logger.error("KeyStore location is empty, cannot find keyStore {} in resources", KEYSTORE_RESOURCE_KEY);
                throw new BlueButtonClientException("Unable to get keystore from resources",
                        new MissingResourceException("", DefaultBlueButtonClient.class.getName(), KEYSTORE_RESOURCE_KEY));
            }
        } else {
            final String keyStorePath = config.getString("aggregation.bbclient.keyStore.location");
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
}
