package gov.cms.dpc.attribution;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.attribution.jdbi.*;
import gov.cms.dpc.attribution.resources.v1.*;
import gov.cms.dpc.attribution.tasks.TruncateDatabase;
import gov.cms.dpc.common.hibernate.attribution.DPCHibernateBundle;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.macaroons.store.hibernate.HibernateKeyStore;
import gov.cms.dpc.macaroons.thirdparty.IThirdPartyKeyStore;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import org.hibernate.SessionFactory;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;

import java.security.SecureRandom;
import java.time.Duration;

@SuppressWarnings("rawtypes")
class AttributionAppModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    private final DPCHibernateBundle<DPCAttributionConfiguration> hibernate;

    public AttributionAppModule(DPCHibernateBundle<DPCAttributionConfiguration> hibernate) {
        this.hibernate = hibernate;
    }

    @Override
    public void configure(Binder binder) {
        // Resources
        binder.bind(V1AttributionResource.class);
        binder.bind(EndpointResource.class);
        binder.bind(PatientResource.class);
        binder.bind(PractitionerResource.class);
        binder.bind(GroupResource.class);
        binder.bind(OrganizationResource.class);

        // DAOs
        binder.bind(EndpointDAO.class);
        binder.bind(OrganizationDAO.class);
        binder.bind(PatientDAO.class);
        binder.bind(ProviderDAO.class);
        binder.bind(RosterDAO.class);
        binder.bind(RelationshipDAO.class);

        // Tasks
        binder.bind(TruncateDatabase.class);

        // Services

    }

    @Provides
    HibernateKeyStore provideRootKeyStore(DPCManagedSessionFactory factory, SecureRandom secureRandom) {
        return new UnitOfWorkAwareProxyFactory(hibernate)
                .create(HibernateKeyStore.class, new Class<?>[]{SessionFactory.class, SecureRandom.class}, new Object[]{factory.getSessionFactory(), secureRandom});
    }

    @Provides
    Duration provideExpiration(DPCAttributionConfiguration config) {
        return config.getExpirationThreshold();
    }

    @Provides
    Settings provideSettings() {
        return new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    }

    @Provides
    // We can suppress this because the SessionFactory is managed
    @SuppressWarnings("CloseableProvides")
    SessionFactory provideSessionFactory(DPCManagedSessionFactory factory) {
        return factory.getSessionFactory();
    }

    @Provides
    @Singleton
    IThirdPartyKeyStore thirdPartyKeyStore() {
        return new MemoryThirdPartyKeyStore();
    }
}
