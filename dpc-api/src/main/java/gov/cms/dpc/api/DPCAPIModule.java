package gov.cms.dpc.api;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.typesafe.config.Config;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.api.annotations.AttributionService;
import gov.cms.dpc.common.annotations.ServiceBaseURL;
import gov.cms.dpc.api.client.AttributionServiceClient;
import gov.cms.dpc.api.resources.TestResource;
import gov.cms.dpc.api.resources.v1.BaseResource;
import gov.cms.dpc.api.resources.v1.DataResource;
import gov.cms.dpc.api.resources.v1.GroupResource;
import gov.cms.dpc.api.resources.v1.JobResource;
import io.dropwizard.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;

public class DPCAPIModule extends DropwizardAwareModule<DPAPIConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DPCAPIModule.class);

    DPCAPIModule() {
        // Not used
    }

    @Override
    public void configure(Binder binder) {

        // Clients
        binder.bind(AttributionEngine.class)
                .to(AttributionServiceClient.class);

        // TODO: This will eventually go away.
        binder.bind(TestResource.class);
        // V1 Resources
        binder.bind(BaseResource.class);
        binder.bind(GroupResource.class);
        binder.bind(JobResource.class);
        binder.bind(DataResource.class);
    }

    @Provides
    @AttributionService
    @Singleton
    public WebTarget provideHttpClient() {
        logger.debug("Connecting to attribution server at {}.", getConfiguration().getAttributionURL());
        return new JerseyClientBuilder(getEnvironment())
                .using(getConfiguration().getHttpClient())
                .build("attribution-service")
                .target(getConfiguration().getAttributionURL());
    }

    @Provides
    public Config provideConfig() {
        return getConfiguration().getConfig();
    }

    @Provides
    @ExportPath
    public String provideExportPath() {
        return getConfiguration().getExportPath();
    }

    @Provides
    @ServiceBaseURL
    public String provideBaseURL(@Context HttpServletRequest request) {
        return String.format("%s://%s:%s", request.getScheme(), request.getServerName(), request.getServerPort());
    }

    @Provides
    @APIV1
    public String provideV1URL(@ServiceBaseURL String baseURL) {
        return baseURL + "/v1";
    }
}
