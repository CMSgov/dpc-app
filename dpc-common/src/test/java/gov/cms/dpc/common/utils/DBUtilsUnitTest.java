package gov.cms.dpc.common.utils;

import gov.cms.dpc.common.exceptions.NotATestEnvironmentException;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SystemStubsExtension.class)
class DBUtilsUnitTest {
	@SystemStub
	private EnvironmentVariables envVars;

	DSLContext context = new DefaultDSLContext(SQLDialect.DEFAULT);

	@Test
	void test_truncateAllTables_will_not_run_in_prod() {
		envVars.set("ENV", "prod");

		assertThrows(NotATestEnvironmentException.class, () -> {
			DBUtils.truncateAllTables(context, "public");
		});
	}

	@Test
	void test_truncateAllTables_will_not_run_in_prod_sbx() {
		envVars.set("ENV", "prod-sbx");

		assertThrows(NotATestEnvironmentException.class, () -> {
			DBUtils.truncateAllTables(context, "public");
		});
	}
}
