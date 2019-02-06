package gov.cms.dpc;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.resources.TestResource;
import gov.cms.dpc.resources.V1BaseResource;

public class DPCAppModule extends DropwizardAwareModule<DPCAppConfiguration> {

    private final FhirContext context;

    DPCAppModule() {
        this.context = FhirContext.forR4();
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(TestResource.class);
        binder.bind(V1BaseResource.class);
    }

    @Provides
    IParser getJsonParser() {
        return this.context.newJsonParser();
    }
}
