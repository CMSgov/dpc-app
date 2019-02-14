package gov.cms.dpc.attribution;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import gov.cms.dpc.attribution.cli.SeedCommand;
import gov.cms.dpc.attribution.engine.AttributionEngineModule;
import io.dropwizard.Application;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
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
                .modules(new AttributionAppModule(), new AttributionEngineModule())
                .build();

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle());
        bootstrap.addBundle(new MigrationsBundle<DPCAttributionConfiguration>() {
            @Override
            public PooledDataSourceFactory getDataSourceFactory(DPCAttributionConfiguration configuration) {
                return configuration.getDatabase();
            }
        });
    }

    @Override
    public void run(DPCAttributionConfiguration configuration, Environment environment) {
        // Not used yet
    }

    @Override
    protected void addDefaultCommands(Bootstrap<DPCAttributionConfiguration> bootstrap) {
        bootstrap.addCommand(new SeedCommand());
    }
}
