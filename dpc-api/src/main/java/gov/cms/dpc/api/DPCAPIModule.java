package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.typesafe.config.Config;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.api.resources.TestResource;
import gov.cms.dpc.api.resources.v1.*;
import gov.cms.dpc.api.tasks.DeleteToken;
import gov.cms.dpc.api.tasks.GenerateClientTokens;
import gov.cms.dpc.api.tasks.ListClientTokens;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.common.annotations.ServiceBaseURL;
import gov.cms.dpc.common.hibernate.auth.DPCAuthHibernateBundle;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.annotations.PublicURL;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.macaroons.thirdparty.IThirdPartyKeyStore;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

public class DPCAPIModule extends DropwizardAwareModule<DPCAPIConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DPCAPIModule.class);

    private final DPCAuthHibernateBundle<DPCAPIConfiguration> authHibernateBundle;

    DPCAPIModule(DPCAuthHibernateBundle<DPCAPIConfiguration> authHibernateBundle) {
        this.authHibernateBundle = authHibernateBundle;
    }

    @Override
    public void configure(Binder binder) {
        // TODO: This will eventually go away.
        binder.bind(TestResource.class);
        // V1 Resources
        binder.bind(BaseResource.class);
        binder.bind(DataResource.class);
        binder.bind(DefinitionResource.class);
        binder.bind(EndpointResource.class);
        binder.bind(GroupResource.class);
        binder.bind(JobResource.class);
        binder.bind(OrganizationResource.class);
        binder.bind(PatientResource.class);
        binder.bind(PractitionerResource.class);

        // DAO
        binder.bind(PublicKeyDAO.class);
        binder.bind(TokenDAO.class);

        // Tasks
        binder.bind(GenerateClientTokens.class);
        binder.bind(ListClientTokens.class);
        binder.bind(DeleteToken.class);

        // Healthchecks
        // TODO: Fix with DPC-538
//        binder.bind(AttributionHealthCheck.class);
    }

    // Since the KeyResource requires access to the Auth DB, we have to manually do the creation and resource injection,
    // in order to ensure that the @UnitOfWork annotations are tied to the correct SessionFactory
    @Provides
    public KeyResource provideKeyResource(PublicKeyDAO dao) {
        return new UnitOfWorkAwareProxyFactory(authHibernateBundle)
                .create(KeyResource.class, new Class<?>[]{PublicKeyDAO.class}, new Object[]{dao});
    }

    @Provides
    public TokenResource provideTokenResource(TokenDAO dao, MacaroonBakery bakery) {
        return new UnitOfWorkAwareProxyFactory(authHibernateBundle)
                .create(TokenResource.class,
                        new Class<?>[]{TokenDAO.class, MacaroonBakery.class, TokenPolicy.class},
                        new Object[]{dao, bakery, this.getConfiguration().getTokenPolicy()});
    }

    @Provides
    @Singleton
    public MetricRegistry provideMetricRegistry() {
        return getEnvironment().metrics();
    }

    @Provides
    public Config provideConfig() {
        return getConfiguration().getConfig();
    }

    @Provides
    @ExportPath
    public String provideExportPath() {
        return getConfiguration().getExportPath();
    }

    @Provides
    @ServiceBaseURL
    public String provideBaseURL() {
        return getConfiguration().getPublicURL();
    }

    @Provides
    @APIV1
    public String provideV1URL() {
        return getConfiguration().getPublicURL() + "/v1";
    }

    @Provides
    @PublicURL
    public String providePublicURL(@ServiceBaseURL String baseURL) {
        return baseURL;
    }

    @Provides
    @Singleton
    IThirdPartyKeyStore thirdPartyKeyStore() {
        return new MemoryThirdPartyKeyStore();
    }

    @Provides
    TokenPolicy providePolicy() {
        return getConfiguration().getTokenPolicy();
    }

    @Provides
    // We can suppress this because the SessionFactory is managed
    @SuppressWarnings("CloseableProvides")
    SessionFactory provideSessionFactory(DPCAuthManagedSessionFactory factory) {
        return factory.getSessionFactory();
    }

    @Provides
    @Singleton
    public IGenericClient provideFHIRClient(FhirContext ctx) {
        logger.info("Connecting to attribution server at {}.", getConfiguration().getAttributionURL());
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        return ctx.newRestfulGenericClient(getConfiguration().getAttributionURL());
    }
}
