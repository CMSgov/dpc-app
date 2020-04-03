package gov.cms.dpc.attribution;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.attribution.service.LookBackService;
import gov.cms.dpc.attribution.service.LookBackServiceImpl;
import gov.cms.dpc.queue.service.DataService;

public class AttributionServiceModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    @Override
    public void configure(Binder binder) {
        //Not used
    }

    @Provides
    LookBackService provideLookBackService(DataService dataService) {
        return getConfiguration().isSkipLookBack() ? (orgId, patientID, providerID, withinMonth) -> true
                : new LookBackServiceImpl(dataService);
    }
}
