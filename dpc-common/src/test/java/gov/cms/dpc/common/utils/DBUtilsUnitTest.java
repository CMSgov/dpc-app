package gov.cms.dpc.common.utils;

import gov.cms.dpc.common.exceptions.NotATestEnvironmentException;
import org.jooq.DSLContext;
import org.jooq.Meta;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(SystemStubsExtension.class)
class DBUtilsUnitTest {
	@SystemStub
	private EnvironmentVariables envVars;

    @Mock
	DSLContext context = new DefaultDSLContext(SQLDialect.DEFAULT);

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

	@Test
	void test_truncateAllTables_will_not_run_in_prod() {
		envVars.set("ENV", "prod");

		assertThrows(NotATestEnvironmentException.class, () ->
                DBUtils.truncateAllTables(context, "public"));
	}

	@Test
	void test_truncateAllTables_will_not_run_in_prod_sbx() {
		envVars.set("ENV", "prod-sbx");

		assertThrows(NotATestEnvironmentException.class, () ->
                DBUtils.truncateAllTables(context, "public"));
	}

    @Test
    void test_truncateAllTables_schema_not_found() {
        envVars.set("ENV", "LOCAL");
        Meta mockedMeta = mock(Meta.class);
        doReturn(mockedMeta).when(context).meta();

        IllegalArgumentException err = assertThrows(IllegalArgumentException.class, () ->
                DBUtils.truncateAllTables(context, "foobar"));
        System.out.println(err.getMessage());
        assertEquals("Cannot find schema 'foobar'", err.getMessage());
    }
}
