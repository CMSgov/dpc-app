package gov.cms.dpc.aggregation;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.qclient.MockQueueClient;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.queue.JobQueue;

import javax.inject.Singleton;

public class AggregationModule extends DropwizardAwareModule<DPCAggregationConfiguration> {

    @Override
    public void configure(Binder binder) {
        binder.bind(AggregationEngine.class);

    }

    @Provides
    public Config provideConfig() {
        return getConfiguration().getConfig();
    }

    @Provides
    @Singleton
    public FhirContext provideSTU3Context() {
        return FhirContext.forDstu3();
    }

    @Provides
    public JobQueue provideJobQueue() {
        // TODO: provide an actual client when it gets implemented
        return new MockQueueClient();
    }

    @Provides
    @ExportPath
    public String provideExportPath() {
        return getConfiguration().getExportPath();
    }
}
