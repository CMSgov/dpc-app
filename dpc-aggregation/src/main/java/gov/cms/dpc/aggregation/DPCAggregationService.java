package gov.cms.dpc.aggregation;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.bluebutton.BlueButtonClientModule;
import gov.cms.dpc.common.hibernate.DPCQueueHibernateBundle;
import gov.cms.dpc.common.hibernate.DPCQueueHibernateModule;
import gov.cms.dpc.common.utils.EnvironmentParser;
import gov.cms.dpc.queue.JobQueueModule;
import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class DPCAggregationService extends Application<DPCAggregationConfiguration> {

    private final DPCQueueHibernateBundle<DPCAggregationConfiguration> hibernateBundle = new DPCQueueHibernateBundle<>();

    public static void main(final String[] args) throws Exception {
        new DPCAggregationService().run(args);
    }

    @Override
    public String getName() {
        return "DPC AggregationManager Service";
    }

    @Override
    public void initialize(Bootstrap<DPCAggregationConfiguration> bootstrap) {
        JerseyGuiceUtils.reset();
        GuiceBundle<DPCAggregationConfiguration> guiceBundle = GuiceBundle.defaultBuilder(DPCAggregationConfiguration.class)
                .modules(new AggregationAppModule(),
                        new DPCQueueHibernateModule<>(hibernateBundle),
                        new JobQueueModule<>(),
                        new BlueButtonClientModule<>())
                .build();

        // The Hibernate bundle must be initialized before Guice.
        // The Hibernate Guice module requires an initialized SessionFactory,
        // so Dropwizard needs to initialize the HibernateBundle first to create the SessionFactory.
        bootstrap.addBundle(hibernateBundle);

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle("dpc.aggregation"));
        bootstrap.addBundle(new MigrationsBundle<DPCAggregationConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(DPCAggregationConfiguration dpcAggregationConfiguration) {
                return dpcAggregationConfiguration.getQueueDatabase();
            }

            @Override
            public String getMigrationsFileName() {
                return "migrations/queue.migrations.xml";
            }
        });
    }

    @Override
    public void run(DPCAggregationConfiguration configuration, Environment environment) {
        EnvironmentParser.getEnvironment("Aggregation");
    }
}
