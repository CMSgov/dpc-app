package gov.cms.dpc.attribution;

import com.google.inject.Binder;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.attribution.jdbi.AttributedPatientsDAO;

class AttributionAppModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    AttributionAppModule() {
        // Not used
    }

    @Override
    public void configure(Binder binder) {

        binder.bind(AttributedPatientsDAO.class);
//        binder.bind(SeedCommand.class);
    }
}
