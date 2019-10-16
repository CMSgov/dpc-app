package gov.cms.dpc.api.auth.macaroons;

import com.github.nitram509.jmacaroons.Macaroon;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.macaroons.BakeryProvider;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.SecureRandom;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(BufferedLoggerHandler.class)
class BakeryTests {

    private static final String ORGANIZATION_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8254";
    private static final String BAD_ORG_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8252";

    private MacaroonBakery bakery;

    @BeforeEach
    void setup() {
        bakery = new BakeryProvider(generateTokenPolicy(), new MemoryRootKeyStore(new SecureRandom()), new MemoryThirdPartyKeyStore(), "http://test.local").get();
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
        final Config config = ConfigFactory.load();
        return ConfigBeanFactory.create(config.getConfig("dpc.api.tokens"), TokenPolicy.class);
    }
}
