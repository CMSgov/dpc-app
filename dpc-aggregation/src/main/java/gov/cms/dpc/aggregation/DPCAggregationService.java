package gov.cms.dpc.aggregation;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClientModule;
import gov.cms.dpc.common.hibernate.DPCHibernateModule;
import gov.cms.dpc.queue.JobQueueModule;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DPCAggregationService extends Application<DPCAggregationConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DPCAggregationService.class);

    public static void main(final String[] args) throws Exception {
        new DPCAggregationService().run(args);
    }

    @Override
    public String getName() {
        return "DPC Aggregation Service";
    }

    @Override
    public void initialize(Bootstrap<DPCAggregationConfiguration> bootstrap) {
        GuiceBundle<DPCAggregationConfiguration> guiceBundle = GuiceBundle.defaultBuilder(DPCAggregationConfiguration.class)
                .modules(new DPCHibernateModule(), new AggregationAppModule(), new JobQueueModule(), new BlueButtonClientModule())
                .build();

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle("dpc.aggregation"));
    }

    @Override
    public void run(DPCAggregationConfiguration configuration, Environment environment) {
        String envVar = "local";
        try {
            envVar = System.getenv("ENV");
        } catch (NullPointerException e) {
            // If ENV isn't set, just ignore it.
        }

        logger.info("Starting Aggregation Service in environment: {}", envVar);
    }
}
