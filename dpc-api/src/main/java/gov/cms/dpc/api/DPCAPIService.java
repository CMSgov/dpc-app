package gov.cms.dpc.api;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.codahale.metrics.jersey2.InstrumentedResourceMethodApplicationListener;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.api.auth.AuthModule;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.cli.DemoCommand;
import gov.cms.dpc.api.cli.organizations.OrganizationCommand;
import gov.cms.dpc.api.cli.tokens.TokenCommand;
import gov.cms.dpc.common.hibernate.attribution.DPCHibernateBundle;
import gov.cms.dpc.common.hibernate.attribution.DPCHibernateModule;
import gov.cms.dpc.common.hibernate.auth.DPCAuthHibernateBundle;
import gov.cms.dpc.common.hibernate.auth.DPCAuthHibernateModule;
import gov.cms.dpc.common.hibernate.queue.DPCQueueHibernateBundle;
import gov.cms.dpc.common.hibernate.queue.DPCQueueHibernateModule;
import gov.cms.dpc.common.utils.EnvironmentParser;
import gov.cms.dpc.fhir.FHIRModule;
import gov.cms.dpc.queue.JobQueueModule;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

public class DPCAPIService extends Application<DPCAPIConfiguration> {

    private final DPCHibernateBundle<DPCAPIConfiguration> hibernateBundle = new DPCHibernateBundle<>();
    private final DPCQueueHibernateBundle<DPCAPIConfiguration> hibernateQueueBundle = new DPCQueueHibernateBundle<>();
    private final DPCAuthHibernateBundle<DPCAPIConfiguration> hibernateAuthBundle = new DPCAuthHibernateBundle<>();

    public static void main(final String[] args) throws Exception {
        new DPCAPIService().run(args);
    }

    @Override
    public String getName() {
        return "DPC API Service";
    }

    @Override
    public void initialize(final Bootstrap<DPCAPIConfiguration> bootstrap) {
        // Setup Guice bundle and module injection
        final GuiceBundle<DPCAPIConfiguration> guiceBundle = setupGuiceBundle();

        // The Hibernate bundle must be initialized before Guice.
        // The Hibernate Guice module requires an initialized SessionFactory,
        // so Dropwizard needs to initialize the HibernateBundle first to create the SessionFactory.
        bootstrap.addBundle(hibernateBundle);
        bootstrap.addBundle(hibernateQueueBundle);
        bootstrap.addBundle(hibernateAuthBundle);

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle("dpc.api"));

        // Wrapper around some of the uglier bundle initialization commands
        setupCustomBundles(bootstrap);

        // Add CLI commands
        addCLICommands(bootstrap);
    }

    @Override
    public void run(final DPCAPIConfiguration configuration,
                    final Environment environment) {
        EnvironmentParser.getEnvironment("API");
        final var listener = new InstrumentedResourceMethodApplicationListener(environment.metrics());
        environment.jersey().getResourceConfig().register(listener);

        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(OrganizationPrincipal.class));
    }

    private GuiceBundle<DPCAPIConfiguration> setupGuiceBundle() {
        // This is required for Guice to load correctly. Not entirely sure why
        // https://github.com/dropwizard/dropwizard/issues/1772
        JerseyGuiceUtils.reset();
        return GuiceBundle.defaultBuilder(DPCAPIConfiguration.class)
                .modules(
                        new DPCHibernateModule<>(hibernateBundle),
                        new DPCQueueHibernateModule<>(hibernateQueueBundle),
                        new DPCAuthHibernateModule<>(hibernateAuthBundle),
                        new AuthModule(),
                        new DPCAPIModule(hibernateAuthBundle),
                        new JobQueueModule<>(),
                        new FHIRModule<>()
                )
                .build();
    }

    private void addCLICommands(final Bootstrap<DPCAPIConfiguration> bootstrap) {
        bootstrap.addCommand(new DemoCommand());
        bootstrap.addCommand(new OrganizationCommand());
        bootstrap.addCommand(new TokenCommand());
    }

    private void setupCustomBundles(final Bootstrap<DPCAPIConfiguration> bootstrap) {
        bootstrap.addBundle(new SwaggerBundle<DPCAPIConfiguration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(DPCAPIConfiguration dpcapiConfiguration) {
                return dpcapiConfiguration.getSwaggerBundleConfiguration();
            }
        });
        bootstrap.addBundle(new MigrationsBundle<DPCAPIConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(DPCAPIConfiguration dpcAPIConfiguration) {
                return dpcAPIConfiguration.getAuthDatabase();
            }

            @Override
            public String getMigrationsFileName() {
                return "migrations/auth.migrations.xml";
            }
        });
    }
}
