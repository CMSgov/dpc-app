package gov.cms.dpc.api.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import gov.cms.dpc.api.converters.BakeryKeyPairSerializer;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;
import gov.cms.dpc.api.models.KeyPairResponse;
import io.dropwizard.servlets.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Singleton
public class GenerateKeyPair extends Task {

    private static final Logger logger = LoggerFactory.getLogger(GenerateKeyPair.class);

    private final Curve25519 instance;
    private final ObjectMapper mapper;

    @Inject
    GenerateKeyPair() {
        super("generate-keypair");
        this.instance = Curve25519.getInstance(Curve25519.BEST);
        this.mapper = new ObjectMapper();
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter printWriter) throws Exception {
        logger.warn("Generating new Bakery Keypair!!!");
        final OffsetDateTime createdOn = OffsetDateTime.now(ZoneOffset.UTC);

        final ImmutableCollection<String> userCollection = parameters.get("user");
        if (userCollection.isEmpty()) {
            throw new WebApplicationException("Must have ID of user generating keypair", Response.Status.BAD_REQUEST);
        }

        final String userID = userCollection.asList().get(0);

        final KeyPairResponse keyPairResponse = new KeyPairResponse();
        keyPairResponse.setCreatedOn(createdOn);
        keyPairResponse.setCreatedBy(userID);
        // Hardcoded, for now.
        keyPairResponse.setAlgorithm("X25519");
        final Curve25519KeyPair keyPair = instance.generateKeyPair();
        keyPairResponse.setKeyPair(new BakeryKeyPair(keyPair.getPublicKey(), keyPair.getPrivateKey()));

        this.mapper.writeValue(printWriter, keyPairResponse);
    }
}
