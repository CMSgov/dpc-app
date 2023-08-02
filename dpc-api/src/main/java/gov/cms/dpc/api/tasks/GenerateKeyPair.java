package gov.cms.dpc.api.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.models.KeyPairResponse;
import gov.cms.dpc.common.utils.EnvironmentParser;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;
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
import java.util.List;
import java.util.Map;

/**
 * Admin task to create a new {@link BakeryKeyPair} for use by the {@link gov.cms.dpc.macaroons.MacaroonBakery} component.
 * This will generated an X25519 keypair that is used to encrypt the third-party caveats.
 * <p>
 * Note: This is not in the `keys` package because it doesn't pertain to the organization public signing keys
 */
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
    public void execute(Map<String, List<String>> parameters, PrintWriter printWriter) throws Exception {
        final String environment = EnvironmentParser.getEnvironment("API", false);

        final OffsetDateTime createdOn = OffsetDateTime.now(ZoneOffset.UTC);

        final List<String> userCollection = parameters.get("user");
        if (userCollection.isEmpty()) {
            throw new WebApplicationException("Must have ID of user generating keypair", Response.Status.BAD_REQUEST);
        }
        final String userID = userCollection.get(0);

        logger.warn("User: {} is generating new Bakery Keypair!!!", userID);

        final KeyPairResponse keyPairResponse = new KeyPairResponse();
        keyPairResponse.setCreatedOn(createdOn);
        keyPairResponse.setCreatedBy(userID);
        // Hardcoded, for now.
        keyPairResponse.setAlgorithm("X25519");
        final Curve25519KeyPair keyPair = instance.generateKeyPair();
        keyPairResponse.setKeyPair(new BakeryKeyPair(keyPair.getPublicKey(), keyPair.getPrivateKey()));
        keyPairResponse.setEnvironment(environment);

        this.mapper.writeValue(printWriter, keyPairResponse);
    }
}
