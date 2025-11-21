package gov.cms.dpc.aggregation.tasks;

import gov.cms.dpc.aggregation.DPCAggregationConfiguration;
import gov.cms.dpc.common.utils.DBUtils;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class TruncateDatabaseUnitTest {
	@Mock
	private DPCAggregationConfiguration config;

	@Mock
	private DataSourceFactory dataSourceFactory;

	@Mock
	private ManagedDataSource managedDataSource;

	@Mock
	private Connection connection;

	@Mock
	DSLContext dslContext;

	@Mock
	private PrintWriter printWriter;

	MockedStatic<DSL> mockedDsl;
	MockedStatic<DBUtils> mockedDBUtils;


	@BeforeEach
    void before() {
		openMocks(this);
		mockedDsl = mockStatic(DSL.class);
		mockedDBUtils = mockStatic(DBUtils.class);
	}

	@AfterEach
    void tearDown() {
		mockedDsl.close();
		mockedDBUtils.close();
	}

	@Test
	void test_TruncateDatabase_can_execute() throws Exception {
		when(config.getConsentDatabase()).thenReturn(dataSourceFactory);
		when(dataSourceFactory.build(null, "truncate-db")).thenReturn(managedDataSource);
		when(managedDataSource.getConnection()).thenReturn(connection);

		mockedDsl.when(() -> DSL.using(any(Connection.class), any(Settings.class))).thenReturn(dslContext);

		TruncateDatabase truncateDatabase = new TruncateDatabase(config);
		truncateDatabase.execute(Map.of(), printWriter);

		// Make sure we call truncate tables
		mockedDBUtils.verify(() -> DBUtils.truncateAllTables(dslContext, "public"));
	}
}
