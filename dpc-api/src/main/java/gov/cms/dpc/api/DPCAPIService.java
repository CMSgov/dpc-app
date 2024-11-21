package gov.cms.dpc.api;

import com.codahale.metrics.jersey3.InstrumentedResourceMethodApplicationListener;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import gov.cms.dpc.api.auth.AuthModule;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.cli.keys.KeyCommand;
import gov.cms.dpc.api.cli.organizations.OrganizationCommand;
import gov.cms.dpc.api.cli.tokens.TokenCommand;
import gov.cms.dpc.api.exceptions.BadRequestExceptionMapper;
import gov.cms.dpc.api.exceptions.ConstraintViolationExceptionMapper;
import gov.cms.dpc.api.exceptions.ForbiddenExceptionMapper;
import gov.cms.dpc.api.exceptions.InternalServerErrorExceptionMapper;
import gov.cms.dpc.bluebutton.BlueButtonClientModule;
import gov.cms.dpc.api.exceptions.JsonParseExceptionMapper;
import gov.cms.dpc.api.exceptions.NotAcceptableExceptionMapper;
import gov.cms.dpc.api.exceptions.NotAuthorizedExceptionMapper;
import gov.cms.dpc.api.exceptions.NotDeSerializableExceptionMapper;
import gov.cms.dpc.api.exceptions.NotFoundExceptionMapper;
import gov.cms.dpc.api.exceptions.UnprocessableEntityExceptionMapper;
import gov.cms.dpc.common.hibernate.attribution.DPCHibernateBundle;
import gov.cms.dpc.common.hibernate.attribution.DPCHibernateModule;
import gov.cms.dpc.common.hibernate.auth.DPCAuthHibernateBundle;
import gov.cms.dpc.common.hibernate.auth.DPCAuthHibernateModule;
import gov.cms.dpc.common.hibernate.queue.DPCQueueHibernateBundle;
import gov.cms.dpc.common.hibernate.queue.DPCQueueHibernateModule;
import gov.cms.dpc.common.logging.DebugLoggingModule;
import gov.cms.dpc.common.logging.filters.GenerateRequestIdFilter;
import gov.cms.dpc.common.logging.filters.LogResponseFilter;
import gov.cms.dpc.common.utils.EnvironmentParser;
import gov.cms.dpc.common.utils.UrlGenerator;
import gov.cms.dpc.fhir.FHIRModule;
import gov.cms.dpc.macaroons.BakeryModule;
import gov.cms.dpc.queue.JobQueueModule;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.health.check.http.HttpHealthCheck;
import io.dropwizard.migrations.MigrationsBundle;
import jakarta.validation.Validator;

import jakarta.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DPCAPIService extends Application<DPCAPIConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(DPCAPIService.class);
    
    private final DPCHibernateBundle<DPCAPIConfiguration> hibernateBundle = new DPCHibernateBundle<>();
    private final DPCQueueHibernateBundle<DPCAPIConfiguration> hibernateQueueBundle = new DPCQueueHibernateBundle<>();
    private final DPCAuthHibernateBundle<DPCAPIConfiguration> hibernateAuthBundle = new DPCAuthHibernateBundle<>(List.of(
            "gov.cms.dpc.macaroons.store.hibernate.entities"));

    @SuppressWarnings("rawtypes")
    private GuiceBundle guiceBundle = null;
    
    public static void main(final String[] args) throws Exception {
        LOG.info("OK Chuck I am going to run the API service with args: " + Arrays.toString(args));
        new DPCAPIService().run(args);
    }
    
    @Override
    public String getName() {
        return "DPC API Service";
    }

    @Override
    public void initialize(final Bootstrap<DPCAPIConfiguration> bootstrap) {
        System.out.println("=========> Initializing DPC API Service...");
        
        // Enable variable substitution with environment variables
        EnvironmentVariableSubstitutor substitutor = new EnvironmentVariableSubstitutor(false);
        SubstitutingSourceProvider provider =
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(), substitutor);
        bootstrap.setConfigurationSourceProvider(provider);

        setupJacksonMapping(bootstrap);
        // Setup Guice bundle and module injection

        System.out.println("============> I am about to set up the guice bundle!");
        // The Hibernate bundle must be initialized before Guice.
        // The Hibernate Guice module requires an initialized SessionFactory,
        // so Dropwizard needs to initialize the HibernateBundle first to create the SessionFactory.
        bootstrap.addBundle(hibernateBundle);
        bootstrap.addBundle(hibernateQueueBundle);
        bootstrap.addBundle(hibernateAuthBundle);

        guiceBundle = GuiceBundle.defaultBuilder(DPCAPIConfiguration.class)
                .modules(                
                        new DebugLoggingModule(),
                        new DPCHibernateModule<>(hibernateBundle),
                        new DPCQueueHibernateModule<>(hibernateQueueBundle),
                        new DPCAuthHibernateModule<>(hibernateAuthBundle),
                        new DPCAPIModule(hibernateAuthBundle),
                        new AuthModule(),
                        new BakeryModule(),
                        new JobQueueModule<>(),
                        new FHIRModule<DPCAPIConfiguration>(),
                        new BlueButtonClientModule<DPCAPIConfiguration>()
                )
                .build();
        System.out.println("============> I set up the guice bundle!");
       
        bootstrap.addBundle(guiceBundle);
        
        // Wrapper around some of the uglier bundle initialization commands
        setupCustomBundles(bootstrap);

        // Add CLI commands
        addCLICommands(bootstrap);
        
        System.out.println("==============> Initialize of DPC API Service is done!!");
    }
    
    @Override
    public void run(final DPCAPIConfiguration configuration,
                    final Environment environment) {
        LOG.info("Starting DPCAPIService run!");

        EnvironmentParser.getEnvironment("API");
            environment.servlets().addFilter("GuiceFilter", GuiceFilter.class).addMappingForUrlPatterns(null, false, "/*");

        if(guiceBundle != null) {
            Injector injector = guiceBundle.getInjector();
            if (injector != null) {
                // Retrieve the ValidatorFactory from the Guice injector
                LOG.info("Awesome, we found an injector with " + injector.getAllBindings().size() + " bindings!");
                ValidatorFactory validatorFactory = injector.getInstance(ValidatorFactory.class);
                if (validatorFactory != null) {
                    Validator v = validatorFactory.getValidator();
                    LOG.info("Found a validator, let's set it in the environment!!! " + v.getClass().getCanonicalName());
                    environment.setValidator(v);
                } else {
                    LOG.error("Guice Injector not found!");
                }
            }
        }

        final var listener = new InstrumentedResourceMethodApplicationListener(environment.metrics());
        environment.jersey().getResourceConfig().register(listener);

        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(OrganizationPrincipal.class));
        environment.jersey().register(new JsonParseExceptionMapper());
        environment.jersey().register(new UnprocessableEntityExceptionMapper());
        environment.jersey().register(new BadRequestExceptionMapper());
        environment.jersey().register(new NotDeSerializableExceptionMapper());
        environment.jersey().register(new ConstraintViolationExceptionMapper());
        environment.jersey().register(new ForbiddenExceptionMapper());
        environment.jersey().register(new InternalServerErrorExceptionMapper());
        environment.jersey().register(new NotAcceptableExceptionMapper());
        environment.jersey().register(new NotAuthorizedExceptionMapper());
        environment.jersey().register(new NotFoundExceptionMapper());
        environment.jersey().register(new GenerateRequestIdFilter(false));
        environment.jersey().register(new LogResponseFilter());

        // Http healthchecks on dependent services
        environment.healthChecks().register("api-self-check",
            new HttpHealthCheck(UrlGenerator.generateVersionUrl(configuration.getServicePort(), configuration.getAppContextPath()))
        );
        environment.healthChecks().register("dpc-attribution", new HttpHealthCheck(configuration.getAttributionHealthCheckURL()));
    }

    private void addCLICommands(final Bootstrap<DPCAPIConfiguration> bootstrap) {
        bootstrap.addCommand(new OrganizationCommand());
        bootstrap.addCommand(new TokenCommand());
        bootstrap.addCommand(new KeyCommand());
    }

    private void setupCustomBundles(final Bootstrap<DPCAPIConfiguration> bootstrap) {
        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public DataSourceFactory getDataSourceFactory(DPCAPIConfiguration dpcAPIConfiguration) {
            System.out.println("============> Connecting to database " + dpcAPIConfiguration.getDatabase().getDriverClass() + " at " + dpcAPIConfiguration.getDatabase().getUrl());
            return dpcAPIConfiguration.getAuthDatabase();
            }

            @Override
            public String getMigrationsFileName() {
                return "migrations/auth.migrations.xml";
            }
        });
    }

    private void setupJacksonMapping(final Bootstrap<DPCAPIConfiguration> bootstrap) {
        // By default, Jackson will ignore @Transient annotated fields. We need to disable this so we can use Hibernate entities for serialization as well.
        // We can still ignore fields using @JsonIgnore
        final Hibernate6Module h6M = new Hibernate6Module();
        h6M.disable(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION);
        bootstrap.getObjectMapper().registerModule(h6M);
        bootstrap.getObjectMapper().disable(DeserializationFeature.WRAP_EXCEPTIONS);
    }
}