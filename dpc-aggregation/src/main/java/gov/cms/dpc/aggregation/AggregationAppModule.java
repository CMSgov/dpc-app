package gov.cms.dpc.aggregation;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.engine.AggregationEngine;
import gov.cms.dpc.aggregation.engine.OperationsConfig;
import gov.cms.dpc.common.annotations.AdditionalPaths;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.common.hibernate.DPCHibernateBundle;
import gov.cms.dpc.queue.models.JobModel;

import javax.inject.Singleton;
import java.util.List;

public class AggregationAppModule extends DropwizardAwareModule<DPCAggregationConfiguration> {

    AggregationAppModule() {
        // Not used
    }

    @Override
    public void configure(Binder binder) {
        binder.requestStaticInjection(DPCHibernateBundle.class);
        binder.bind(AggregationEngine.class);
        binder.bind(AggregationManager.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public FhirContext provideSTU3Context() {
        final var fhirContext = FhirContext.forDstu3();

        // Setup the context with model scans (avoids doing this on the fetch threads and perhaps multithreaded bug)
        for(var resourceType: JobModel.validResourceTypes) {
            fhirContext.getResourceDefinition(resourceType.name());
        }
        fhirContext.getResourceDefinition("Bundle");
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
    @AdditionalPaths
    public List<String> provideAdditionalPaths() {
        return List.of("gov.cms.dpc.queue.models");
    }

    @Provides
    OperationsConfig provideOperationsConfig() {
        final var config = getConfiguration();

        return new OperationsConfig(config.getResourcesPerFileCount(),
                config.getExportPath(),
                config.getRetryCount(),
                config.isEncryptionEnabled(),
                config.isParallelEnabled(),
                config.getWriteThreadFactor(),
                config.getFetchThreadFactor());
    }
}
