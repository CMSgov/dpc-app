package gov.cms.dpc.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import gov.cms.dpc.api.auth.jwt.IJTICache;
import gov.cms.dpc.api.auth.jwt.JwtKeyLocator;
import gov.cms.dpc.api.converters.ChecksumConverterProvider;
import gov.cms.dpc.api.converters.HttpRangeHeaderParamConverterProvider;
import gov.cms.dpc.api.core.FileManager;
import gov.cms.dpc.api.jdbi.IpAddressDAO;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.api.resources.v1.*;
import gov.cms.dpc.api.tasks.GenerateKeyPair;
import gov.cms.dpc.api.tasks.keys.DeletePublicKey;
import gov.cms.dpc.api.tasks.keys.ListPublicKeys;
import gov.cms.dpc.api.tasks.keys.UploadPublicKey;
import gov.cms.dpc.api.tasks.tokens.DeleteToken;
import gov.cms.dpc.api.tasks.tokens.GenerateClientTokens;
import gov.cms.dpc.api.tasks.tokens.ListClientTokens;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.common.annotations.JobTimeout;
import gov.cms.dpc.common.annotations.ServiceBaseURL;
import gov.cms.dpc.common.hibernate.auth.DPCAuthHibernateBundle;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import gov.cms.dpc.fhir.configuration.FHIRClientConfiguration;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.annotations.PublicURL;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.macaroons.thirdparty.IThirdPartyKeyStore;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import gov.cms.dpc.queue.service.DataService;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

public class DPCAPIModule extends DropwizardAwareModule<DPCAPIConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DPCAPIModule.class);

    private final DPCAuthHibernateBundle<DPCAPIConfiguration> authHibernateBundle;

    DPCAPIModule(DPCAuthHibernateBundle<DPCAPIConfiguration> authHibernateBundle) {
        this.authHibernateBundle = authHibernateBundle;
    }

    @Override
    public void configure() {
        Binder binder = binder();
        // V1 Resources
        binder.bind(BaseResource.class);
        binder.bind(DataResource.class);
        binder.bind(DefinitionResource.class);
        binder.bind(GroupResource.class);
        binder.bind(JobResource.class);
        binder.bind(PatientResource.class);
        binder.bind(PractitionerResource.class);

        // DAO
        binder.bind(PublicKeyDAO.class);
        binder.bind(TokenDAO.class);
        binder.bind(IpAddressDAO.class);

        // Tasks
        binder.bind(GenerateClientTokens.class);
        binder.bind(GenerateKeyPair.class);
        binder.bind(ListClientTokens.class);
        binder.bind(DeleteToken.class);
        binder.bind(UploadPublicKey.class);
        binder.bind(ListPublicKeys.class);
        binder.bind(DeletePublicKey.class);

        binder.bind(FileManager.class);
        binder.bind(HttpRangeHeaderParamConverterProvider.class);
        binder.bind(ChecksumConverterProvider.class);

        binder.bind(DataService.class);

        // Healthchecks
        // Additional health-checks can be added here.
        // By default, Dropwizard adds a check for Hibernate and each additional database (e.g. auth, queue, etc).
        // We also get the JobQueueStatus by default, even though it always return healthy.
        // Http health checks on dependent services are in the service's run method.
    }

    // Since the KeyResource requires access to the Auth DB, we have to manually do the creation and resource injection,
    // in order to ensure that the @UnitOfWork annotations are tied to the correct SessionFactory
    @Provides
    public KeyResource provideKeyResource(PublicKeyDAO dao) {
        return new UnitOfWorkAwareProxyFactory(authHibernateBundle)
                .create(KeyResource.class, new Class<?>[]{PublicKeyDAO.class}, new Object[]{dao});
    }

    @Provides
    public TokenResource provideTokenResource(TokenDAO dao, MacaroonBakery bakery, JwtKeyLocator locator, IJTICache cache, @APIV1 String publicURL) {
        return new UnitOfWorkAwareProxyFactory(authHibernateBundle)
                .create(TokenResource.class,
                        new Class<?>[]{TokenDAO.class,
                                MacaroonBakery.class,
                                TokenPolicy.class,
                                JwtKeyLocator.class,
                                IJTICache.class,
                                String.class},
                        new Object[]{dao,
                                bakery,
                                this.configuration().getTokenPolicy(),
                                locator,
                                cache,
                                publicURL});
    }

    @Provides
    public IpAddressResource provideIpAddressResource(IpAddressDAO dao) {
        return new UnitOfWorkAwareProxyFactory(authHibernateBundle)
            .create(IpAddressResource.class, new Class<?>[]{IpAddressDAO.class}, new Object[]{dao});
    }

    @Provides
    public OrganizationResource provideOrganizationResource(@Named("attribution") IGenericClient client, TokenDAO tokenDAO, PublicKeyDAO keyDAO) {
        return new UnitOfWorkAwareProxyFactory(authHibernateBundle)
                .create(OrganizationResource.class,
                        new Class<?>[]{IGenericClient.class,
                        TokenDAO.class,
                        PublicKeyDAO.class},
                        new Object[]{client, tokenDAO, keyDAO});
    }

    @Provides
    @Singleton
    public MetricRegistry provideMetricRegistry() {
        return environment().metrics();
    }

    @Provides
    @ExportPath
    public String provideExportPath() {
        return configuration().getExportPath();
    }

    @Provides
    @ServiceBaseURL
    public String provideBaseURL() {
        return configuration().getPublicURL();
    }

    @Provides
    @APIV1
    public String provideV1URL() {
        return configuration().getPublicURL() + "/v1";
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
        return configuration().getTokenPolicy();
    }

    @Provides
    // We can suppress this because the SessionFactory is managed
    @SuppressWarnings("CloseableProvides")
    SessionFactory provideSessionFactory(DPCAuthManagedSessionFactory factory) {
        return factory.getSessionFactory();
    }

    @Provides
    @Singleton
    @Named("fhirContextAttributionSTU3")
    public FhirContext provideAttributionSTU3Context() {
        return FhirContext.forDstu3();
    }

    @Provides
    @Singleton
    @Named("attribution")
    public IGenericClient provideFHIRClient(@Named("fhirContextAttributionSTU3") FhirContext ctx) {
        FHIRClientConfiguration fhirClientConfiguration = configuration().getFhirClientConfiguration();

        String attributionUrl = fhirClientConfiguration.getServerBaseUrl();
        logger.info("Connecting to attribution server at {}.", attributionUrl);
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        ctx.getRestfulClientFactory().setSocketTimeout(fhirClientConfiguration.getTimeouts().getSocketTimeout());
        ctx.getRestfulClientFactory().setConnectTimeout(fhirClientConfiguration.getTimeouts().getConnectionTimeout());
        ctx.getRestfulClientFactory().setConnectionRequestTimeout(fhirClientConfiguration.getTimeouts().getRequestTimeout());

        IGenericClient client = ctx.newRestfulGenericClient(attributionUrl);
        client.registerInterceptor(new RequestIdHeaderInterceptor());
        return client;
    }

    @Provides
    @JobTimeout
    public int provideJobTimeoutInSeconds() {
        return configuration().getJobTimeoutInSeconds();
    }

    @Provides
    @Singleton
    @Named("defaultPageSize")
    int provideDefaultPageSize() { return configuration().getFHIRConfiguration().getDefaultPageSize(); }
}
