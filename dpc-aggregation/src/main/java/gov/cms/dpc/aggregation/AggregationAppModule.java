package gov.cms.dpc.aggregation;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.engine.AggregationEngine;
import gov.cms.dpc.aggregation.qclient.MockEmptyQueueClient;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.queue.JobQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

public class AggregationAppModule extends DropwizardAwareModule<DPCAggregationConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(AggregationAppModule.class);

    AggregationAppModule() {

    }

    @Override
    public void configure(Binder binder) {
        binder.bind(AggregationEngine.class);
        binder.bind(Aggregation.class).asEagerSingleton();

    }

    @Provides
    @Singleton
    public FhirContext provideSTU3Context() {
        return FhirContext.forDstu3();
    }

    @Provides
    public JobQueue provideJobQueue() {
        // TODO: provide an actual client when it gets implemented
        return new MockEmptyQueueClient();
    }

    @Provides
    @ExportPath
    public String provideExportPath() {
        return getConfiguration().getExportPath();
    }

    @Provides
    public Config provideConfig() {
        return getConfiguration().getConfig();
    }
}
