package gov.cms.dpc.consent;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractConsentTest {
    private static final String KEY_PREFIX = "dpc.consent";
    protected static final DropwizardTestSupport<DPCConsentConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCConsentService.class, null, ConfigOverride.config(KEY_PREFIX, "", ""));

    @BeforeAll
    public static void initDB() throws Exception {
        APPLICATION.before();
    }

    @AfterAll
    public static void shutdown() {
        APPLICATION.after();
    }
}
