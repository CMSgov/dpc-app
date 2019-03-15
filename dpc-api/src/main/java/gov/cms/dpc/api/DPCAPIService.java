package gov.cms.dpc.api;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.fhir.FHIRModule;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClientModule;
import gov.cms.dpc.queue.JobQueueModule;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class DPCAPIService extends Application<DPAPIConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DPCAPIService().run(args);
    }

    @Override
    public String getName() {
        return "DPC API Service";
    }

    @Override
    public void initialize(final Bootstrap<DPAPIConfiguration> bootstrap) {
        // This is required for Guice to load correctly. Not entirely sure why
        // https://github.com/dropwizard/dropwizard/issues/1772
        JerseyGuiceUtils.reset();
        GuiceBundle<DPAPIConfiguration> guiceBundle = GuiceBundle.defaultBuilder(DPAPIConfiguration.class)
                .modules(new DPCAPIModule(), new JobQueueModule(), new FHIRModule(), new BlueButtonClientModule())
                .build();

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle());
    }

    @Override
    public void run(final DPAPIConfiguration configuration,
                    final Environment environment) {
        // Not used yet
    }
}
