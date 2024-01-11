package gov.cms.dpc.aggregation;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.bluebutton.BlueButtonClientModule;
import gov.cms.dpc.common.hibernate.attribution.DPCHibernateBundle;
import gov.cms.dpc.common.hibernate.attribution.DPCHibernateModule;
import gov.cms.dpc.common.hibernate.queue.DPCQueueHibernateBundle;
import gov.cms.dpc.common.hibernate.queue.DPCQueueHibernateModule;
import gov.cms.dpc.common.utils.EnvironmentParser;
import gov.cms.dpc.queue.JobQueueModule;
import io.dropwizard.core.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import ru.vyarus.dropwizard.guice.GuiceBundle;

public class DPCAggregationService extends Application<DPCAggregationConfiguration> {

    private final DPCQueueHibernateBundle<DPCAggregationConfiguration> queueHibernateBundle = new DPCQueueHibernateBundle<>();
    private final DPCHibernateBundle<DPCAggregationConfiguration> hibernateBundle = new DPCHibernateBundle<>();

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
        GuiceBundle guiceBundle = GuiceBundle.builder()
                .modules(new AggregationAppModule(),
                        new DPCQueueHibernateModule<>(queueHibernateBundle),
                        new DPCHibernateModule<>(hibernateBundle),
                        new JobQueueModule<DPCAggregationConfiguration>(),
                        new BlueButtonClientModule<DPCAggregationConfiguration>())
                .build();

        // The Hibernate bundle must be initialized before Guice.
        // The Hibernate Guice module requires an initialized SessionFactory,
        // so Dropwizard needs to initialize the HibernateBundle first to create the SessionFactory.
        bootstrap.addBundle(queueHibernateBundle);
        bootstrap.addBundle(hibernateBundle);

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle("dpc.aggregation"));
        bootstrap.addBundle(new MigrationsBundle<>() {
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
