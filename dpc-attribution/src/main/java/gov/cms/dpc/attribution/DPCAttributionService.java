package gov.cms.dpc.attribution;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class DPCAttributionService extends Application<DPCAttributionConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DPCAttributionService().run(args);
    }

    @Override
    public String getName() {
        return "DPC Attribution Service";
    }

    @Override
    public void initialize(Bootstrap<DPCAttributionConfiguration> bootstrap) {
        GuiceBundle<DPCAttributionConfiguration> guiceBundle = GuiceBundle.defaultBuilder(DPCAttributionConfiguration.class)
                .modules(new AttributionModule())
                .build();

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle());
    }

    @Override
    public void run(DPCAttributionConfiguration configuration, Environment environment) {
        // Not used yet
    }
}
