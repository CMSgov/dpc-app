package gov.cms.dpc.attribution.jobs;

import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.jobs.Job;
import org.jooq.conf.Settings;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.DPCAttributionService;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.JobTestUtils;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Group;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.jdbc3.Jdbc3ConnectionPool;
import org.h2.jdbcx.JdbcConnectionPool;
import org.quartz.JobExecutionContext;
import ru.vyarus.dropwizard.guice.module.context.SharedConfigurationState;

import javax.ws.rs.client.Client;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static gov.cms.dpc.attribution.AttributionTestHelpers.DEFAULT_ORG_ID;
import static gov.cms.dpc.attribution.SharedMethods.createAttributionBundle;
import static gov.cms.dpc.attribution.SharedMethods.submitAttributionBundle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Integration test for verifying that the expiration jobs runs correctly.
 * We currently don't have a way of verifying that the job runs when expected, since we can't really override Dropwizard's time source.
 * In the future, we might consider using something like ByteBuddy to intercept all system time calls and see if the job still gets run.
 * <p>
 * Disabled until made effective
 */
@ExtendWith(BufferedLoggerHandler.class)
@ExtendWith(MockitoExtension.class)
class ExpirationJobTestUnit {

    @Mock
    private ManagedDataSource dataSource;
    @Mock
    private Settings settings;
    @Mock
    private Connection connection;
    @Mock
    private JobExecutionContext jobContext;
    private FhirContext ctx = FhirContext.forDstu3();
    private static final String PROVIDER_ID = "2322222227";
    @InjectMocks
    private ExpireAttributions expireAttributions;

    @BeforeEach
    public void setUp() throws SQLException, NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);

        JdbcConnectionPool cp = JdbcConnectionPool.create("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        this.connection = cp.getConnection();
        when(this.dataSource.getConnection()).thenReturn(this.connection);

        Field dataSourceField = ExpireAttributions.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        dataSourceField.set(expireAttributions, dataSource);

        try (Statement stmt = this.connection.createStatement()) {
            stmt.execute("CREATE TABLE attribution (id BIGINT PRIMARY KEY, period_begin TIMESTAMP WITH TIME ZONE, period_end TIMESTAMP WITH TIME ZONE DEFAULT (now()), inactive BOOLEAN DEFAULT (false))");
            stmt.execute("INSERT INTO attribution (id, period_begin, period_end) VALUES (1, now(), DATEADD('DAY', -1, now()))");
        }
    }

    @Test
    void testDoJob() throws NoSuchFieldException, SQLException {
        assertEquals(this.expireAttributions.getClass(), ExpireAttributions.class);

        // Create and submit mock attribution bundle
        // Need to experiment more with how this works
        // final Bundle updateBundle = createAttributionBundle(PROVIDER_ID, "0L00L00LL00", DEFAULT_ORG_ID);
        // final Group group = submitAttributionBundle(client, updateBundle);
        // Mock attributions for testing
        // Run job and check that attributions are expired or deleted as appropriate
        this.expireAttributions.doJob(this.jobContext);
        try (Statement smt = this.connection.createStatement()) {
            ResultSet result = smt.executeQuery("SELECT inactive FROM attribution WHERE id = 1");
            if (result.next()) {
                String isInactive = result.getString("inactive");
                assertEquals(isInactive, true);
            }
        }
    }
}
