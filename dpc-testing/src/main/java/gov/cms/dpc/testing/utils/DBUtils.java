package gov.cms.dpc.testing.utils;

import org.jooq.DSLContext;
import org.jooq.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DBUtils {

    private static final Logger logger = LoggerFactory.getLogger(DBUtils.class);

    private DBUtils() {
        // Not used
    }

    /**
     * Truncate all data in a specific {@link Schema}
     *
     * @param context - {@link DSLContext} to use
     * @param schema  - {@link String} name of schema to truncate
     */
    public static void truncateAllTables(DSLContext context, String schema) {
        logger.debug("Truncating schema: {}", schema);
        final List<Schema> schemas = context.meta()
                .getSchemas(schema);

        if (schemas.isEmpty()) {
            throw new IllegalArgumentException(String.format("Cannot find schema '%s'", schema));
        }

        // Truncate all the tables (except for the liquibase metadata)
        schemas
                .get(0)
                .getTables()
                .stream()
                .filter(table -> !table.getName().startsWith("databasechangelog"))
                .forEach(table -> {
                    logger.trace("Truncating table: {}", table.getName());
                    context.truncate(table).cascade().execute();
                });
    }
}
