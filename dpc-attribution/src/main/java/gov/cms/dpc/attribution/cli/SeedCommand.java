package gov.cms.dpc.attribution.cli;

import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.engine.TestSeeder;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.MissingResourceException;

public class SeedCommand extends ConfiguredCommand<DPCAttributionConfiguration> {

    private static Logger logger = LoggerFactory.getLogger(SeedCommand.class);
    private static final String CSV = "test_associations.csv";


    public SeedCommand() {
        super("seed", "Seed the attribution roster");
    }

    @Override
    protected void run(Bootstrap<DPCAttributionConfiguration> bootstrap, Namespace namespace, DPCAttributionConfiguration configuration) throws Exception {
        logger.info("Running!");

        // Get the db factory
        final PooledDataSourceFactory dataSourceFactory = configuration.getDatabase();
        dataSourceFactory.asSingleConnectionPool();
        final ManagedDataSource dataSource = dataSourceFactory.build(new MetricRegistry(), "attribution-seeder");

        // Read in the seeds file and write things
        logger.info("Seeding attributions");
        // Get the test seeds
        final InputStream resource = TestSeeder.class.getClassLoader().getResourceAsStream(CSV);
        if (resource == null) {
            throw new MissingResourceException("Can not find seeds file", this.getClass().getName(), CSV);
        }

        try (final Connection connection = dataSource.getConnection()) {
            connection.beginRequest();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    final String[] splits = line.split(",");
                    logger.debug("Associating {} with {}.", splits[1], splits[0]);
                    try (Statement statement = connection.createStatement()) {
                        final String query = String.format("INSERT INTO attributions (provider_id, patient_id) VALUES %s, %s", splits[1], splits[0]);
                        statement.execute(query);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        logger.info("Finished loading seeds");

    }
}
