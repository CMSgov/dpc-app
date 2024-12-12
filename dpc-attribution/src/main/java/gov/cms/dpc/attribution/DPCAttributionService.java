package gov.cms.dpc.attribution;

import com.codahale.metrics.jersey3.InstrumentedResourceMethodApplicationListener;
import com.google.inject.servlet.GuiceFilter;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import gov.cms.dpc.attribution.cli.SeedCommand;
import gov.cms.dpc.attribution.jobs.ExpireAttributions;
import gov.cms.dpc.common.hibernate.attribution.DPCHibernateBundle;
import gov.cms.dpc.common.hibernate.attribution.DPCHibernateModule;
import gov.cms.dpc.common.logging.filters.GenerateRequestIdFilter;
import gov.cms.dpc.common.logging.filters.LogResponseFilter;
import gov.cms.dpc.common.utils.EnvironmentParser;
import gov.cms.dpc.common.utils.UrlGenerator;
import gov.cms.dpc.fhir.FHIRModule;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.health.check.http.HttpHealthCheck;
import io.dropwizard.jobs.GuiceJobManager;
import io.dropwizard.jobs.JobsBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DPCAttributionService extends Application<DPCAttributionConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DPCAttributionService.class);

    private final DPCHibernateBundle<DPCAttributionConfiguration> hibernateBundle = new DPCHibernateBundle<>();

    @SuppressWarnings("rawtypes")
    private GuiceBundle guiceBundle;

    public static void main(final String[] args) throws Exception {
        logger.info("Running the attribution service with args: " + Arrays.toString(args));
        new DPCAttributionService().run(args);
    }

    @Override
    public String getName() {
        return "DPC Attribution Service";
    }

    @Override
    public void initialize(Bootstrap<DPCAttributionConfiguration> bootstrap) {

        // Enable variable substitution with environment variables
        EnvironmentVariableSubstitutor substitutor = new EnvironmentVariableSubstitutor(false);
        SubstitutingSourceProvider provider =
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(), substitutor);
        bootstrap.setConfigurationSourceProvider(provider);

        registerBundles(bootstrap);

        bootstrap.addCommand(new SeedCommand(bootstrap.getApplication()));
    }

    @Override
    public void run(DPCAttributionConfiguration configuration, Environment environment) {
        logger.info("Starting DPCAttributionService run!");
        EnvironmentParser.getEnvironment("Attribution");

        GuiceJobManager jobManager = new GuiceJobManager(configuration, guiceBundle.getInjector());
        environment.lifecycle().manage(jobManager);

        environment.servlets().addFilter("GuiceFilter", GuiceFilter.class).addMappingForUrlPatterns(null, false, "/*");

        final var listener = new InstrumentedResourceMethodApplicationListener(environment.metrics());
        environment.jersey().getResourceConfig().register(listener);
        environment.jersey().register(new GenerateRequestIdFilter(true));
        environment.jersey().register(new LogResponseFilter());

        // Http health checks
        environment.healthChecks().register("attribution-self-check",
            new HttpHealthCheck(UrlGenerator.generateVersionUrl(configuration.getServicePort()))
        );
    }

    private void registerBundles(Bootstrap<DPCAttributionConfiguration> bootstrap) {
        guiceBundle = GuiceBundle.defaultBuilder(DPCAttributionConfiguration.class)
                .modules(
                        new DPCHibernateModule<>(hibernateBundle),
                        new AttributionAppModule(),
                        new FHIRModule<DPCAttributionConfiguration>())
                .build();

        // The Hibernate bundle must be initialized before Guice.
        // The Hibernate Guice module requires an initialized SessionFactory,
        // so Dropwizard needs to initialize the HibernateBundle first to create the SessionFactory.
        bootstrap.addBundle(hibernateBundle);
        bootstrap.addBundle(guiceBundle);

        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public PooledDataSourceFactory getDataSourceFactory(DPCAttributionConfiguration configuration) {
                return configuration.getDatabase();
            }
        });

        bootstrap.addBundle(new SwaggerBundle<>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(DPCAttributionConfiguration configuration) {
                return configuration.getSwaggerBundleConfiguration();
            }
        });

        bootstrap.addBundle(new JobsBundle(new ExpireAttributions()));
    }
}
