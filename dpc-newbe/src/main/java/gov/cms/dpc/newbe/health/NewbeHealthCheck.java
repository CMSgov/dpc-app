package gov.cms.dpc.newbe.health;

import gov.cms.dpc.newbe.client.NewbeClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import ru.vyarus.dropwizard.guice.module.installer.feature.health.NamedHealthCheck;

@Singleton
public class NewbeHealthCheck extends NamedHealthCheck {

    private final NewbeClient client;

    @Inject
    public NewbeHealthCheck(NewbeClient client) {
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
        return "newbe-client";
    }
}
