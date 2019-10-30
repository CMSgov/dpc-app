package gov.cms.dpc.consent;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.common.hibernate.consent.DPCConsentManagedSessionFactory;
import gov.cms.dpc.consent.jdbi.ConsentDAO;
import org.hibernate.SessionFactory;

public class ConsentAppModule extends DropwizardAwareModule<DPCConsentConfiguration> {

    @Override
    public void configure(Binder binder) {
        binder.bind(ConsentDAO.class);
    }

    @Provides
    // We can suppress this because the SessionFactory is managed
    @SuppressWarnings("CloseableProvides")
    public SessionFactory provideSessionFactory(DPCConsentManagedSessionFactory factory) {
        return factory.getSessionFactory();
    }
}
