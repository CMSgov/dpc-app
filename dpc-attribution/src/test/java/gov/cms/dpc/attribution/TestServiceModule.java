package gov.cms.dpc.attribution;

import com.google.inject.Binder;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.attribution.service.LookBackService;

public class TestServiceModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    private LookBackService lookBackService;
    public TestServiceModule(LookBackService lookBackService) {
        this.lookBackService = lookBackService;
    }
    @Override
    public void configure(Binder binder) {
        binder.bind(LookBackService.class).toInstance(lookBackService);


    }
}
