package gov.cms.dpc.aggregation;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.dao.OrganizationDAO;
import gov.cms.dpc.aggregation.dao.RosterDAO;
import gov.cms.dpc.aggregation.engine.AggregationEngine;
import gov.cms.dpc.aggregation.engine.JobBatchProcessor;
import gov.cms.dpc.aggregation.engine.OperationsConfig;
import gov.cms.dpc.aggregation.service.EveryoneGetsDataLookBackServiceImpl;
import gov.cms.dpc.aggregation.service.LookBackService;
import gov.cms.dpc.aggregation.service.LookBackServiceImpl;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.common.annotations.JobTimeout;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.fhir.hapi.ContextUtils;
import gov.cms.dpc.queue.models.JobQueueBatch;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;

import javax.inject.Singleton;

public class AggregationAppModule extends DropwizardAwareModule<DPCAggregationConfiguration> {


    AggregationAppModule() {
        // Not used
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(AggregationEngine.class);
        binder.bind(AggregationManager.class).asEagerSingleton();
        binder.bind(JobBatchProcessor.class);
        binder.bind(RosterDAO.class);
        binder.bind(OrganizationDAO.class);

        // Healthchecks
        // Additional health-checks can be added here
        // By default, Dropwizard adds a check for Hibernate and each additonal database (e.g. auth, queue, etc)
        // We also have JobQueueHealthy which ensures the queue is operation correctly
        // We have the BlueButton Client healthcheck as well
    }

    @Provides
    @Singleton
    public FhirContext provideSTU3Context() {
        final var fhirContext = FhirContext.forDstu3();

        // Setup the context with model scans (avoids doing this on the fetch threads and perhaps multithreaded bug)
        ContextUtils.prefetchResourceModels(fhirContext, JobQueueBatch.validResourceTypes);
        return fhirContext;
    }

    @Provides
    @Singleton
    MetricRegistry provideMetricRegistry() {
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
    OperationsConfig provideOperationsConfig() {
        final var config = getConfiguration();

        return new OperationsConfig(
                config.getResourcesPerFileCount(),
                config.getExportPath(),
                config.getRetryCount(),
                config.getPollingFrequency(),
                config.getLookBackMonths(),
                config.getLookBackDate()
        );
    }

    @Provides
    @JobTimeout
    public int provideJobTimeoutInSeconds() {
        return getConfiguration().getJobTimeoutInSeconds();
    }

    @Provides
    LookBackService provideLookBackService(DPCManagedSessionFactory sessionFactory, RosterDAO rosterDAO, OrganizationDAO organizationDAO, OperationsConfig operationsConfig) {
        //Configuring to skip look back when look back months is less than 0
        if (operationsConfig.getLookBackMonths() < 0) {
            return new EveryoneGetsDataLookBackServiceImpl();
        }
        return new UnitOfWorkAwareProxyFactory("roster", sessionFactory.getSessionFactory()).create(LookBackServiceImpl.class,
                new Class<?>[]{RosterDAO.class, OrganizationDAO.class, OperationsConfig.class},
                new Object[]{rosterDAO, organizationDAO, operationsConfig});
    }
}
