package gov.cms.dpc.api;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.api.cli.DemoCommand;
import gov.cms.dpc.common.hibernate.DPCHibernateModule;
import gov.cms.dpc.fhir.FHIRModule;
import gov.cms.dpc.queue.JobQueueModule;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class DPCAPIService extends Application<DPCAPIConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DPCAPIService().run(args);
    }

    @Override
    public String getName() {
        return "DPC API Service";
    }

    @Override
    public void initialize(final Bootstrap<DPCAPIConfiguration> bootstrap) {
        // This is required for Guice to load correctly. Not entirely sure why
        // https://github.com/dropwizard/dropwizard/issues/1772
        JerseyGuiceUtils.reset();
        GuiceBundle<DPCAPIConfiguration> guiceBundle = GuiceBundle.defaultBuilder(DPCAPIConfiguration.class)
                .modules(new DPCHibernateModule(), new DPCAPIModule(), new JobQueueModule(), new FHIRModule())
                .build();

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle("dpc.api"));

        bootstrap.addCommand(new DemoCommand());
    }

    @Override
    public void run(final DPCAPIConfiguration configuration,
                    final Environment environment) {
        // Not used yet
    }
}
