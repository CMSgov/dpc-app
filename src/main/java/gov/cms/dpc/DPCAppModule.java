package gov.cms.dpc;

import com.google.inject.Binder;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.resources.BaseResource;

public class DPCAppModule extends DropwizardAwareModule<DPCAppConfiguration> {

    @Override
    public void configure(Binder binder) {
        binder.bind(BaseResource.class);
    }
}
