package gov.cms.dpc.consent;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.codahale.metrics.jersey2.InstrumentedResourceMethodApplicationListener;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.common.hibernate.consent.DPCConsentHibernateBundle;
import gov.cms.dpc.common.hibernate.consent.DPCConsentHibernateModule;
import gov.cms.dpc.common.utils.EnvironmentParser;
import gov.cms.dpc.consent.cli.ConsentCommands;
import gov.cms.dpc.consent.cli.SeedCommand;
import gov.cms.dpc.fhir.FHIRModule;
import io.dropwizard.Application;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import liquibase.exception.DatabaseException;
import org.knowm.dropwizard.sundial.SundialBundle;
import org.knowm.dropwizard.sundial.SundialConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.GuiceBundle;

import java.sql.SQLException;


public class DPCConsentService extends Application<DPCConsentConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DPCConsentService.class);
    private final DPCConsentHibernateBundle<DPCConsentConfiguration> hibernateBundle = new DPCConsentHibernateBundle<>();

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

        GuiceBundle guiceBundle = GuiceBundle.builder()
                .modules(
                        new DPCConsentHibernateModule<>(hibernateBundle),
                        new FHIRModule<DPCConsentConfiguration>(),
                        new ConsentAppModule())
                .build();

        bootstrap.addBundle(hibernateBundle);
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle("dpc.consent"));
        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public PooledDataSourceFactory getDataSourceFactory(DPCConsentConfiguration configuration) {
                logger.debug("Connecting to database {} at {}", configuration.getConsentDatabase().getDriverClass(), configuration.getConsentDatabase().getUrl());
                return configuration.getConsentDatabase();
            }

            @Override
            public String getMigrationsFileName() {
                return "consent.migrations.xml";
            }
        });
        bootstrap.addBundle(new SundialBundle<>() {
            @Override
            public SundialConfiguration getSundialConfiguration(DPCConsentConfiguration dpcConsentConfiguration) {
                return dpcConsentConfiguration.getSundial();
            }
        });
        bootstrap.addCommand(new SeedCommand(bootstrap.getApplication()));
        bootstrap.addCommand(new ConsentCommands());
    }

    @Override
    public void run(DPCConsentConfiguration configuration, Environment environment) throws DatabaseException, SQLException {
        EnvironmentParser.getEnvironment("Consent");
        final var listener = new InstrumentedResourceMethodApplicationListener(environment.metrics());
        environment.jersey().getResourceConfig().register(listener);
    }
}
