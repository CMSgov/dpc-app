package gov.cms.dpc.attribution;

import com.google.inject.Binder;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.attribution.service.DataService;
import gov.cms.dpc.attribution.service.LookBackService;

public class TestServiceModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    private LookBackService lookBackService;
    private DataService dataService;
    public TestServiceModule(LookBackService lookBackService, DataService dataService) {
        this.lookBackService = lookBackService;
        this.dataService = dataService;
    }
    @Override
    public void configure(Binder binder) {
        binder.bind(DataService.class).toInstance(dataService);
        binder.bind(LookBackService.class).toInstance(lookBackService);


    }
}
