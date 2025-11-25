package gov.cms.dpc.common.utils;


import gov.cms.dpc.common.exceptions.NotATestEnvironmentException;
import org.jooq.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
@SuppressWarnings("deprecation")
class DBUtilsUnitTest {
	@SystemStub
	private EnvironmentVariables envVars;

    // We MUST use the exact deprecated interfaces the production code uses
    // @SuppressWarnings is added to the class for cleaner compilation
    private DSLContext context = mock(DSLContext.class);
    private Meta meta = mock(Meta.class);
    private Schema schema = mock(Schema.class);
    private Table table1 = mock(Table.class);
    private Table table2 = mock(Table.class);

    private TruncateIdentityStep truncateIdentityStep = mock(TruncateIdentityStep.class);
    private TruncateCascadeStep cascadeStep = mock(TruncateCascadeStep.class);


    @BeforeEach
    void setup() {
        // Reset mocks before each test
        reset(context, meta, schema, table1, table2, truncateIdentityStep, cascadeStep);

        // Standard mock setup for truncateAllTables logic
        when(context.meta()).thenReturn(meta);
        when(table1.getName()).thenReturn("patient");
        when(table2.getName()).thenReturn("provider");

        // Mock the deprecated truncate chain: context.truncate(table).cascade().execute()
        when(context.truncate(any(Table.class))).thenReturn(truncateIdentityStep);
        when(truncateIdentityStep.cascade()).thenReturn(cascadeStep);
        when(cascadeStep.execute()).thenReturn(1);
    }

	@Test
	void test_truncateAllTables_will_not_run_in_prod() {
		envVars.set("ENV", "prod");

		assertThrows(NotATestEnvironmentException.class, () -> {
			DBUtils.truncateAllTables(context, "public");
		});

        // Verify no database interaction happened
        verify(context, never()).meta();
	}

	@Test
	void test_truncateAllTables_will_not_run_in_sandbox() {
		envVars.set("ENV", "sandbox");

		assertThrows(NotATestEnvironmentException.class, () -> {
			DBUtils.truncateAllTables(context, "public");
		});

        // Verify no database interaction happened
        verify(context, never()).meta();
	}

    @Test
    void test_private_constructor_is_covered() throws Exception {
        // Use reflection to instantiate the private constructor for coverage
        java.lang.reflect.Constructor<DBUtils> constructor = DBUtils.class.getDeclaredConstructor();
        assertTrue(constructor.isAccessible() || constructor.trySetAccessible());
        constructor.newInstance();
    }

    @Test
    void test_truncateAllTables_succeeds_in_local() {
        envVars.set("ENV", "LOCAL");
        when(meta.getSchemas(anyString())).thenReturn(List.of(schema));
        when(schema.getTables()).thenReturn(List.of(table1, table2));

        DBUtils.truncateAllTables(context, "public");

        // Verify both tables were truncated
        verify(context).truncate(table1);
        verify(context).truncate(table2);

        // Verify execution was called on the final step for both
        verify(cascadeStep, times(2)).execute();
        verify(meta, times(1)).getSchemas("public");
    }

    @Test
    void test_truncateAllTables_skips_liquibase_tables() {
        // Arrange
        envVars.set("ENV", "LOCAL");

        Table changelogTable = mock(Table.class);
        when(changelogTable.getName()).thenReturn("databasechangelog");

        when(meta.getSchemas(anyString())).thenReturn(List.of(schema));
        when(schema.getTables()).thenReturn(List.of(table1, changelogTable));

        DBUtils.truncateAllTables(context, "public");

        // Verify only table1 was truncated
        verify(context).truncate(table1);

        // Verify changelogTable was NOT truncated
        verify(context, never()).truncate(changelogTable);
        verify(context, never()).truncate(table2);
        verify(cascadeStep, times(1)).execute();
    }

    @Test
    void test_truncateAllTables_throws_on_invalid_schema() {
        envVars.set("ENV", "LOCAL");
        when(meta.getSchemas(anyString())).thenReturn(List.of()); // Mock empty list

        // Verify IllegalArgumentException exception thrown
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            DBUtils.truncateAllTables(context, "invalid_schema");
        });

        assertTrue(e.getMessage().contains("Cannot find schema 'invalid_schema'"));
    }
}
