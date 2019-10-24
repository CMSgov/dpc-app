package gov.cms.dpc.consent;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.codahale.metrics.jersey2.InstrumentedResourceMethodApplicationListener;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.common.hibernate.attribution.DPCHibernateBundle;
import gov.cms.dpc.common.utils.EnvironmentParser;
import io.dropwizard.Application;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import liquibase.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;


public class DPCConsentService extends Application<DPCConsentConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DPCConsentService.class);
    private final DPCHibernateBundle<DPCConsentConfiguration> hibernateBundle = new DPCHibernateBundle<>();

    public static void main(final String[] args) throws Exception {
        new DPCConsentService().run(args);
    }

    @Override
    public String getName() {
        return "DPC Consent Service";
    }

    @Override
    public void initialize(Bootstrap<DPCConsentConfiguration> bootstrap) {
        JerseyGuiceUtils.reset();

        GuiceBundle<DPCConsentConfiguration> guiceBundle = GuiceBundle.defaultBuilder(DPCConsentConfiguration.class)
                .modules(new ConsentAppModule())
                .build();

        bootstrap.addBundle(hibernateBundle);
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle("dpc.consent"));
        bootstrap.addBundle(new MigrationsBundle<DPCConsentConfiguration>() {
            @Override
            public PooledDataSourceFactory getDataSourceFactory(DPCConsentConfiguration configuration) {
                logger.debug("Connecting to database {} at {}", configuration.getDatabase().getDriverClass(), configuration.getDatabase().getUrl());
                return configuration.getDatabase();
            }
        });
    }

    @Override
    public void run(DPCConsentConfiguration configuration, Environment environment) throws DatabaseException, SQLException {
        EnvironmentParser.getEnvironment("Consent");
        final var listener = new InstrumentedResourceMethodApplicationListener(environment.metrics());
        environment.jersey().getResourceConfig().register(listener);
    }
}
