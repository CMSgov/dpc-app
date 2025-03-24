package gov.cms.dpc.api.auth.macaroons;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.macaroons.BakeryProvider;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.security.SecureRandom;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(BufferedLoggerHandler.class)
class BakeryAuthTest {

    private static final String ORGANIZATION_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8254";
    private static final String BAD_ORG_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8252";

    private MacaroonBakery bakery;

    @BeforeEach
    void setup() {
        BakeryProvider provider = new BakeryProvider(generateTokenPolicy(), new MemoryRootKeyStore(new SecureRandom()), new MemoryThirdPartyKeyStore(), "http://test.local", BakeryKeyPair.generate());
        bakery = assertDoesNotThrow(provider::get);
    }


    @Test
    void testOrganizationToken() {

        final Macaroon macaroon = bakery
                .createMacaroon(Collections.singletonList(
                        new MacaroonCaveat(new MacaroonCondition("organization_id",
                                MacaroonCondition.Operator.EQ, ORGANIZATION_ID))));

        assertThrows(BakeryException.class, () -> bakery.verifyMacaroon(Collections.singletonList(macaroon), String.format("organization_id = %s", BAD_ORG_ID)));
    }

    private TokenPolicy generateTokenPolicy() {
        Yaml yaml = new Yaml(new Constructor(TokenPolicy.class, new LoaderOptions()));
        return yaml.load(BakeryAuthTest.class.getClassLoader().getResourceAsStream("token_policy.yml"));
    }
}
