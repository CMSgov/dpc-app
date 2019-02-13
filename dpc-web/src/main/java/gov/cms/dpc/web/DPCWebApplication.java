package gov.cms.dpc.web;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import gov.cms.dpc.attribution.AttributionModule;
import gov.cms.dpc.queue.JobQueueModule;
import gov.cms.dpc.web.features.FHIRRequestFeature;
import gov.cms.dpc.web.filters.FHIRServletResponse;
import io.dropwizard.Application;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.server.Server;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

public class DPCWebApplication extends Application<DPWebConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DPCWebApplication().run(args);
    }

    @Override
    public String getName() {
        return "DPC Web API";
    }

    @Override
    public void initialize(final Bootstrap<DPWebConfiguration> bootstrap) {
        GuiceBundle<DPWebConfiguration> guiceBundle = GuiceBundle.defaultBuilder(DPWebConfiguration.class)
                .modules(new DPCAppModule(), new JobQueueModule(), new AttributionModule())
                .build();

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle());
    }

    @Override
    public void run(final DPWebConfiguration configuration,
                    final Environment environment) {
        // Add FHIR filters
        environment.jersey().register(FHIRRequestFeature.class);
//        environment.jersey().register(new FHIRExceptionHandler());
//        environment.servlets().addFilter("FHIR", new FHIRServletResponse()).addMappingForUrlPatterns(EnumSet.of(DispatcherType.ERROR), true,"/*");
    }
}
