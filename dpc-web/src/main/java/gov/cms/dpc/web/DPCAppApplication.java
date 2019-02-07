package gov.cms.dpc.web;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class DPCAppApplication extends Application<DPCAppConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DPCAppApplication().run(args);
    }

    @Override
    public String getName() {
        return "DPCApp";
    }

    @Override
    public void initialize(final Bootstrap<DPCAppConfiguration> bootstrap) {
        GuiceBundle<DPCAppConfiguration> guiceBundle = GuiceBundle.defaultBuilder(DPCAppConfiguration.class)
                .modules(new DPCAppModule())
                .build();

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle());
    }

    @Override
    public void run(final DPCAppConfiguration configuration,
                    final Environment environment) {
        // TODO: implement application
    }

}
