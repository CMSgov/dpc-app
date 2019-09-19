package gov.cms.dpc.aggregation;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.engine.AggregationEngine;
import gov.cms.dpc.aggregation.engine.OperationsConfig;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.fhir.hapi.ContextUtils;
import gov.cms.dpc.queue.models.JobQueueBatch;

import javax.inject.Singleton;

public class AggregationAppModule extends DropwizardAwareModule<DPCAggregationConfiguration> {

    AggregationAppModule() {
        // Not used
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(AggregationEngine.class);
        binder.bind(AggregationManager.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public FhirContext provideSTU3Context() {
        final var fhirContext = FhirContext.forDstu3();

        // Setup the context with model scans (avoids doing this on the fetch threads and perhaps multithreaded bug)
        ContextUtils.prefetchResourceModels(fhirContext, JobQueueBatch.validResourceTypes);
        return fhirContext;
    }

    @Provides
    @Singleton
    MetricRegistry provideMetricRegistry() {
        return getEnvironment().metrics();
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
    OperationsConfig provideOperationsConfig() {
        final var config = getConfiguration();

        return new OperationsConfig(config.getResourcesPerFileCount(),
                config.getExportPath(),
                config.getRetryCount());
    }
}
