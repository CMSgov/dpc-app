package gov.cms.dpc.attribution.cli;

import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.dao.tables.Organizations;
import gov.cms.dpc.attribution.dao.tables.Patients;
import gov.cms.dpc.attribution.dao.tables.Providers;
import gov.cms.dpc.attribution.jdbi.RosterUtils;
import gov.cms.dpc.common.utils.SeedProcessor;
import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.jooq.DSLContext;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.MissingResourceException;

public class SeedCommand extends EnvironmentCommand<DPCAttributionConfiguration> {

    private static Logger logger = LoggerFactory.getLogger(SeedCommand.class);
    private static final String CSV = "test_associations.csv";

    private final Settings settings;


    public SeedCommand(Application<DPCAttributionConfiguration> application) {
        super(application, "seed", "Seed the attribution roster");
        this.settings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    }

    @Override
    public void configure(Subparser subparser) {
        subparser
                .addArgument("-t", "--timestamp")
                .dest("timestamp")
                .type(String.class)
                .required(false)
                .help("Custom timestamp to use when adding attributed relationships.");
    }

    @Override
    protected void run(Environment environment, Namespace namespace, DPCAttributionConfiguration configuration) throws Exception {
        // Get the db factory
        final PooledDataSourceFactory dataSourceFactory = configuration.getDatabase();
        final ManagedDataSource dataSource = dataSourceFactory.build(environment.metrics(), "attribution-seeder");

        final OffsetDateTime creationTimestamp = generateTimestamp(namespace);

        // Read in the seeds file and write things
        logger.info("Seeding attributions at time {}", creationTimestamp.toLocalDateTime());
        // Get the test seeds

        try (DSLContext context = DSL.using(dataSource.getConnection(), this.settings);
        InputStream resources = SeedCommand.class.getClassLoader().getResourceAsStream(CSV)) {
            if (resources == null) {
                throw new MissingResourceException("Can not find seeds file", this.getClass().getName(), CSV);
            }

            // Truncate everything
            context.truncate(Patients.PATIENTS).cascade().execute();
            context.truncate(Providers.PROVIDERS).cascade().execute();
            context.truncate(Organizations.ORGANIZATIONS).cascade().execute();

            final var seedProcessor = new SeedProcessor(resources);
            seedProcessor
                    .extractProviderMap()
                    .entrySet()
                    .stream()
                    .map(seedProcessor::generateRosterBundle)
                    .forEach(bundle -> RosterUtils.submitAttributionBundle(bundle, context, creationTimestamp));
            logger.info("Finished loading seeds");
        }
    }

    private static OffsetDateTime generateTimestamp(Namespace namespace) {
        final String timestamp = namespace.getString("timestamp");
        if (timestamp == null) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        return OffsetDateTime.parse(timestamp.trim());
    }
}
