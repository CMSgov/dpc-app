package gov.cms.dpc.consent;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import org.hibernate.SessionFactory;

public class ConsentAppModule extends DropwizardAwareModule<DPCConsentConfiguration> {

    @Override
    public void configure(Binder binder) {
        binder.bind(ConsentDAO.class);
    }

    @Provides
    public String provideSuppressionFileDir(DPCConsentConfiguration config) {
        return config.getSuppressionFileDir();
    }

    @Provides
    // We can suppress this because the SessionFactory is managed
    @SuppressWarnings("CloseableProvides")
    public SessionFactory provideSessionFactory(DPCManagedSessionFactory factory) {
        return factory.getSessionFactory();
    }
}
