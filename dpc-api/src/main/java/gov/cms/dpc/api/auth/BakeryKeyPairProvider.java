package gov.cms.dpc.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.models.KeyPairResponse;
import gov.cms.dpc.common.utils.EnvironmentParser;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class BakeryKeyPairProvider implements Provider<BakeryKeyPair> {

    private static final Logger logger = LoggerFactory.getLogger(BakeryKeyPairProvider.class);

    private final DPCAPIConfiguration config;
    private final ObjectMapper mapper;

    BakeryKeyPairProvider(DPCAPIConfiguration config) {
        this.config = config;
        this.mapper = new ObjectMapper();
    }

    @Override
    public BakeryKeyPair get() {
        final String location = config.getKeyPairLocation();
        logger.info("Loading keypair from {}", location);

        final File file = new File(location);
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            final KeyPairResponse keyPairResponse = this.mapper.readValue(reader, KeyPairResponse.class);
            validateEnvironment(keyPairResponse);
            return keyPairResponse.getKeyPair();
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Cannot load key pair from %s", location), e);
        }
    }

    private void validateEnvironment(KeyPairResponse response) {
        if (!EnvironmentParser.getEnvironment("API", false).equals(response.getEnvironment())) {
            throw new IllegalStateException("Cannot load keypair for different environment");
        }
    }
}
