package gov.cms.dpc.oneecosystem.health;

import gov.cms.dpc.oneecosystem.client.OneEcosystemClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import ru.vyarus.dropwizard.guice.module.installer.feature.health.NamedHealthCheck;

@Singleton
public class OneEcosystemHealthCheck extends NamedHealthCheck {

    private final OneEcosystemClient client;

    @Inject
    public OneEcosystemHealthCheck(OneEcosystemClient client) {
        this.client = client;
    }

    @Override
    protected Result check() {
        try {
            if (client.healthy()) {
                return Result.healthy();
            }
            return Result.unhealthy("BCDA endpoint is unhealthy");
        } catch (Exception e) {
            return Result.unhealthy(e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "one-ecosystem-client";
    }
}
