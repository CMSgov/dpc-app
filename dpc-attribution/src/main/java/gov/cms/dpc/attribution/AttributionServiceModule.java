package gov.cms.dpc.attribution;

import com.google.inject.Binder;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.attribution.service.LookBackService;

public class AttributionServiceModule extends DropwizardAwareModule<DPCAttributionConfiguration> {
    @Override
    public void configure(Binder binder) {
        binder.bind(LookBackService.class);
    }
}
