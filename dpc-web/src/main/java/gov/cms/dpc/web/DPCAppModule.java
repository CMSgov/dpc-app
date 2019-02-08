package gov.cms.dpc.web;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.web.resources.TestResource;
import gov.cms.dpc.web.resources.v1.BaseResource;
import gov.cms.dpc.web.resources.v1.GroupResource;

public class DPCAppModule extends DropwizardAwareModule<DPWebConfiguration> {

    private final FhirContext context;

    DPCAppModule() {
        this.context = FhirContext.forR4();
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(TestResource.class);
        // V1 Resources
        binder.bind(BaseResource.class);
        binder.bind(GroupResource.class);

    }

    @Provides
    IParser getJsonParser() {
        return this.context.newJsonParser();
    }
}
