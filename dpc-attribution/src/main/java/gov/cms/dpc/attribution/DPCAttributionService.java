package gov.cms.dpc.attribution;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.attribution.cli.SeedCommand;
import gov.cms.dpc.fhir.FHIRModule;
import io.dropwizard.Application;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DPCAttributionService extends Application<DPCAttributionConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DPCAttributionService.class);

    public static void main(final String[] args) throws Exception {
        new DPCAttributionService().run(args);
    }

    @Override
    public String getName() {
        return "DPC Attribution Service";
    }

    @Override
    public void initialize(Bootstrap<DPCAttributionConfiguration> bootstrap) {
        // This is required for Guice to load correctly. Not entirely sure why
        // https://github.com/dropwizard/dropwizard/issues/1772
        JerseyGuiceUtils.reset();
        GuiceBundle<DPCAttributionConfiguration> guiceBundle = GuiceBundle.defaultBuilder(DPCAttributionConfiguration.class)
                .modules(new AttributionAppModule(),
                        new FHIRModule())
                .build();

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle("dpc.attribution"));
        bootstrap.addBundle(new MigrationsBundle<DPCAttributionConfiguration>() {
            @Override
            public PooledDataSourceFactory getDataSourceFactory(DPCAttributionConfiguration configuration) {
                logger.debug("Connection to database {} at {}", configuration.getDatabase().getDriverClass(), configuration.getDatabase().getUrl());
                return configuration.getDatabase();
            }
        });

        bootstrap.addCommand(new SeedCommand(bootstrap.getApplication()));
    }

    @Override
    public void run(DPCAttributionConfiguration configuration, Environment environment) {
        // Not used yet
    }
}
