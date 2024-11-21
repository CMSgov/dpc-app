package gov.cms.dpc.consent;

import com.codahale.metrics.jersey3.InstrumentedResourceMethodApplicationListener;
import com.google.inject.servlet.GuiceFilter;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import gov.cms.dpc.common.hibernate.consent.DPCConsentHibernateBundle;
import gov.cms.dpc.common.hibernate.consent.DPCConsentHibernateModule;
import gov.cms.dpc.common.utils.EnvironmentParser;
import gov.cms.dpc.common.utils.UrlGenerator;
import gov.cms.dpc.consent.cli.ConsentCommands;
import gov.cms.dpc.consent.cli.SeedCommand;
import gov.cms.dpc.fhir.FHIRModule;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.health.check.http.HttpHealthCheck;
import io.dropwizard.migrations.MigrationsBundle;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import liquibase.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;


public class DPCConsentService extends Application<DPCConsentConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DPCConsentService.class);
    private final DPCConsentHibernateBundle<DPCConsentConfiguration> hibernateBundle = new DPCConsentHibernateBundle<>();

    public static void main(final String[] args) throws Exception {
        logger.info("OK Chuck I am going to run the consent service with args: " + Arrays.toString(args));
        new DPCConsentService().run(args);
    }

    @Override
    public String getName() {
        return "DPC Consent Service";
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void initialize(Bootstrap<DPCConsentConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        EnvironmentVariableSubstitutor substitutor = new EnvironmentVariableSubstitutor(false);
        SubstitutingSourceProvider provider =
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(), substitutor);
        bootstrap.setConfigurationSourceProvider(provider);

        System.out.println("============> I am about to set up the guice bundle!");
        GuiceBundle guiceBundle = GuiceBundle.defaultBuilder(DPCConsentConfiguration.class)
                .modules(new DPCConsentHibernateModule<>(hibernateBundle),
                        new FHIRModule<DPCConsentConfiguration>(),
                        new ConsentAppModule()
                )
                .build();
        System.out.println("============> I set up the guice bundle!");

        bootstrap.addBundle(hibernateBundle);
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public PooledDataSourceFactory getDataSourceFactory(DPCConsentConfiguration configuration) {
                System.out.println("============> Connecting to database " + configuration.getConsentDatabase().getDriverClass() + " at " + configuration.getConsentDatabase().getUrl());
                return configuration.getConsentDatabase();
            }

            @Override
            public String getMigrationsFileName() {
                return "consent.migrations.xml";
            }
        });
        bootstrap.addBundle(new SwaggerBundle<>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(DPCConsentConfiguration configuration) {
                return configuration.getSwaggerBundleConfiguration();
            }
        });

        bootstrap.addCommand(new SeedCommand(bootstrap.getApplication()));
        bootstrap.addCommand(new ConsentCommands());
        
        System.out.println("==============> Initialize of DPC Consent Service is done!!");
    }

    @Override
    public void run(DPCConsentConfiguration configuration, Environment environment) throws DatabaseException, SQLException {
        logger.info("Starting DPCConsentService run!");
        EnvironmentParser.getEnvironment("Consent");
        
        environment.servlets().addFilter("GuiceFilter", GuiceFilter.class).addMappingForUrlPatterns(null, false, "/*");

        final var listener = new InstrumentedResourceMethodApplicationListener(environment.metrics());
        environment.jersey().getResourceConfig().register(listener);

        logger.info("Chuck here is a checkpoint!");
        // Http health checks
        environment.healthChecks().register("consent-self-check",
            new HttpHealthCheck(UrlGenerator.generateVersionUrl(configuration.getServicePort()))
        );
        
        logger.info("The run method is done!");
    }
}
