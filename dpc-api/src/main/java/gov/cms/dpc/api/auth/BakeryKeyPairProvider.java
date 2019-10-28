package gov.cms.dpc.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return this.mapper.readValue(reader, BakeryKeyPair.class);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Cannot load key pair from %s", location), e);
        }
    }
}
