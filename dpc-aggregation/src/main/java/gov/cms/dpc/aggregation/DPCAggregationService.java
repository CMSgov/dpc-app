package gov.cms.dpc.aggregation;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClientModule;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class DPCAggregationService extends Application<DPCAggregationConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DPCAggregationService().run(args);

    }

    @Override
    public String getName() {
        return "DPC Aggregation Service";
    }

    @Override
    public void initialize(Bootstrap<DPCAggregationConfiguration> bootstrap) {
        JerseyGuiceUtils.reset();
        GuiceBundle<DPCAggregationConfiguration> guiceBundle = GuiceBundle.defaultBuilder(DPCAggregationConfiguration.class)
                .modules(new AggregationModule(), new BlueButtonClientModule())
                .build();

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle("dpc.aggregation"));
    }

    @Override
    public void run(DPCAggregationConfiguration configuration, Environment environment) {

    }
}
