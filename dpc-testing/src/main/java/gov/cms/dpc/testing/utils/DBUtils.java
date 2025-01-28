package gov.cms.dpc.testing.utils;

import gov.cms.dpc.testing.exceptions.DumbEngineerException;
import org.jooq.DSLContext;
import org.jooq.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

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
        checkEnv();

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

    private static void checkEnv() {
        Optional<String> envOptional = Optional.ofNullable(System.getenv("ENV"));
        envOptional.ifPresent(env -> {
            if( env.toUpperCase().contains("PROD") ) {
                throw new DumbEngineerException("Do you really want to truncate a DB in " + env + "?");
            }
        });
    }
}
