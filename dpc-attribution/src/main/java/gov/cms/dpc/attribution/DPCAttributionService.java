package gov.cms.dpc.attribution;

import com.codahale.metrics.jersey2.InstrumentedResourceMethodApplicationListener;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.attribution.cli.SeedCommand;
import gov.cms.dpc.attribution.jobs.ExpireAttributions;
import gov.cms.dpc.common.hibernate.attribution.DPCHibernateBundle;
import gov.cms.dpc.common.hibernate.attribution.DPCHibernateModule;
import gov.cms.dpc.common.logging.filters.GenerateRequestIdFilter;
import gov.cms.dpc.common.logging.filters.LogResponseFilter;
import gov.cms.dpc.common.utils.EnvironmentParser;
import gov.cms.dpc.fhir.FHIRModule;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jobs.GuiceJobsBundle;
import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.JobsBundle;
import io.dropwizard.migrations.MigrationsBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.GuiceBundle;

public class DPCAttributionService extends Application<DPCAttributionConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DPCAttributionService.class);

    private final DPCHibernateBundle<DPCAttributionConfiguration> hibernateBundle = new DPCHibernateBundle<>();

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
        registerBundles(bootstrap);

        bootstrap.addCommand(new SeedCommand(bootstrap.getApplication()));
    }

    @Override
    public void run(DPCAttributionConfiguration configuration, Environment environment) {
        EnvironmentParser.getEnvironment("Attribution");
        final var listener = new InstrumentedResourceMethodApplicationListener(environment.metrics());
        environment.jersey().getResourceConfig().register(listener);
        environment.jersey().register(new GenerateRequestIdFilter(true));
        environment.jersey().register(new LogResponseFilter());
    }

    private void registerBundles(Bootstrap<DPCAttributionConfiguration> bootstrap) {
        GuiceBundle guiceBundle = GuiceBundle.builder()
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
        GuiceJobsBundle guiceJobsBundle = new GuiceJobsBundle(guiceBundle.getInjector());
        bootstrap.addBundle(guiceJobsBundle);
        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public PooledDataSourceFactory getDataSourceFactory(DPCAttributionConfiguration configuration) {
                logger.debug("Connecting to database {} at {}", configuration.getDatabase().getDriverClass(), configuration.getDatabase().getUrl());
                return configuration.getDatabase();
            }
        });
        Job expireAttributionsJob = new ExpireAttributions();
        bootstrap.addBundle(new JobsBundle(expireAttributionsJob));
    }
}
