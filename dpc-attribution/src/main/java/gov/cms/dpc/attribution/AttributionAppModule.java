package gov.cms.dpc.attribution;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.attribution.jdbi.*;
import gov.cms.dpc.attribution.macaroons.BakeryProvider;
import gov.cms.dpc.attribution.resources.v1.*;
import gov.cms.dpc.attribution.tasks.TruncateDatabase;
import gov.cms.dpc.common.annotations.AdditionalPaths;
import gov.cms.dpc.common.hibernate.DPCHibernateBundle;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import org.hibernate.SessionFactory;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;

import java.time.Duration;
import java.util.List;

@SuppressWarnings("rawtypes")
class AttributionAppModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    AttributionAppModule() {
    }

    @Override
    public void configure(Binder binder) {
        binder.requestStaticInjection(DPCHibernateBundle.class);

        // Resources
        binder.bind(V1AttributionResource.class);
        binder.bind(EndpointResource.class);
        binder.bind(PatientResource.class);
        binder.bind(PractitionerResource.class);
        binder.bind(GroupResource.class);

        // DAOs
        binder.bind(EndpointDAO.class);
        binder.bind(OrganizationDAO.class);
        binder.bind(PatientDAO.class);
        binder.bind(ProviderDAO.class);
        binder.bind(RosterDAO.class);

        // Tasks
        binder.bind(TruncateDatabase.class);

        // Services
        binder.bind(MacaroonBakery.class).toProvider(BakeryProvider.class);
    }

    @Provides
    OrganizationResource provideOrganizationResource(DPCHibernateBundle hibernate, OrganizationDAO dao, MacaroonBakery bakery) {
        return new UnitOfWorkAwareProxyFactory(hibernate)
                .create(OrganizationResource.class, new Class<?>[]{OrganizationDAO.class, MacaroonBakery.class}, new Object[]{dao, bakery});
    }

    @Provides
    RelationshipDAO provideRelationshipDAO(DPCHibernateBundle hibernateModule, DPCManagedSessionFactory factory) {
        return new UnitOfWorkAwareProxyFactory(hibernateModule)
                .create(RelationshipDAO.class, SessionFactory.class, factory);
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
    @AdditionalPaths
    List<String> provideAdditionalPaths() {
        return List.of("gov.cms.dpc.macaroons.store.hibernate.entities");
    }
}
