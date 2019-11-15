package gov.cms.dpc.consent;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.consent.jdbi.ConsentDAO;

public class ConsentAppModule extends DropwizardAwareModule<DPCConsentConfiguration> {

    @Override
    public void configure(Binder binder) {
        binder.bind(ConsentDAO.class);
    }

    @Provides
    public String provideSuppressionFileDir() {
        return this.getConfiguration().getSuppressionFileDir();
    }
}
