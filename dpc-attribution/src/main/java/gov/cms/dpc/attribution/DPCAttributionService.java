package gov.cms.dpc.attribution;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.attribution.cli.SeedCommand;
import gov.cms.dpc.common.hibernate.DPCHibernateModule;
import gov.cms.dpc.fhir.FHIRModule;
import gov.cms.dpc.macaroons.BakeryModule;
import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.knowm.dropwizard.sundial.SundialBundle;
import org.knowm.dropwizard.sundial.SundialConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DPCAttributionService extends Application<DPCAttributionConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DPCAttributionService.class);

    public static void main(final String[] args) throws Exception {
        new DPCAttributionService().run(args);
    }

    @Override
    public String getName() {
        return "DPC Attribution Service";
    }

    @Override
    public void initialize(Bootstrap<DPCAttributionConfiguration> bootstrap) {
        // This is required for Guice to load correctly. Not entirely sure why
        // https://github.com/dropwizard/dropwizard/issues/1772
        JerseyGuiceUtils.reset();
        GuiceBundle<DPCAttributionConfiguration> guiceBundle = GuiceBundle.defaultBuilder(DPCAttributionConfiguration.class)
                .modules(new AttributionAppModule(),
                        new DPCHibernateModule<>(),
                        new FHIRModule(),
                        new BakeryModule())
                .build();

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle("dpc.attribution"));
        bootstrap.addBundle(new MigrationsBundle<DPCAttributionConfiguration>() {
            @Override
            public PooledDataSourceFactory getDataSourceFactory(DPCAttributionConfiguration configuration) {
                logger.debug("Connecting to database {} at {}", configuration.getDatabase().getDriverClass(), configuration.getDatabase().getUrl());
                return configuration.getDatabase();
            }
        });

        final SundialBundle<DPCAttributionConfiguration> sundialBundle = new SundialBundle<>() {
            @Override
            public SundialConfiguration getSundialConfiguration(DPCAttributionConfiguration dpcAttributionConfiguration) {
                return dpcAttributionConfiguration.getSundial();
            }
        };

        bootstrap.addBundle(sundialBundle);

        bootstrap.addCommand(new SeedCommand(bootstrap.getApplication()));
    }

    @Override
    public void run(DPCAttributionConfiguration configuration, Environment environment) throws DatabaseException, SQLException {
        migrateDatabase(configuration, environment);
    }

    private void migrateDatabase(DPCAttributionConfiguration configuration, Environment environment) throws SQLException {
        if (configuration.getMigrationEnabled()) {
            logger.info("Migrating Database Schema");
            final ManagedDataSource dataSource = createMigrationDataSource(configuration, environment);

            try (final Connection connection = dataSource.getConnection()) {
                final JdbcConnection conn = new JdbcConnection(connection);

                final Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(conn);
                final Liquibase liquibase = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), database);
                liquibase.update("");
            } catch (LiquibaseException e) {
                throw new IllegalStateException("Unable to migrate database", e);
            } finally {
                try {
                    dataSource.stop();
                } catch (Exception e) {
                    logger.error("Unable to stop migration datasource", e);
                }
            }
        } else {
            logger.info("Skipping Database Migration");
        }
    }

    private ManagedDataSource createMigrationDataSource(DPCAttributionConfiguration configuration, Environment environment) {
        final DataSourceFactory dataSourceFactory = configuration.getDatabase();
        return dataSourceFactory.build(environment.metrics(), "migration-ds");
    }
}
