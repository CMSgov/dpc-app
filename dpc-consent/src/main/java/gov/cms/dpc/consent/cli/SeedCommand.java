package gov.cms.dpc.consent.cli;

import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.consent.DPCConsentConfiguration;
import gov.cms.dpc.consent.dao.tables.records.ConsentRecord;
import io.dropwizard.core.Application;
import io.dropwizard.core.cli.EnvironmentCommand;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.core.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import org.jooq.DSLContext;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.MissingResourceException;
import java.util.UUID;
import java.util.stream.Stream;

import static gov.cms.dpc.consent.dao.tables.Consent.CONSENT;

public class SeedCommand extends EnvironmentCommand<DPCConsentConfiguration> {
    private static Logger logger = LoggerFactory.getLogger(SeedCommand.class);
    private static final String CSV = "test_consent.csv";

    private final Settings settings;

    public SeedCommand(Application<DPCConsentConfiguration> application) {
        super(application, "seed", "Seed the consent database");
        this.settings = new Settings().withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, DPCConsentConfiguration dpcConsentConfiguration) throws Exception {
        URL resource = SeedCommand.class.getClassLoader().getResource(CSV);
        if (resource == null || resource.getPath().isEmpty()) {
            throw new MissingResourceException("Cannot find consent seeds file", this.getClass().getName(), CSV);
        }

        final PooledDataSourceFactory dataSourceFactory = dpcConsentConfiguration.getConsentDatabase();
        final ManagedDataSource dataSource = dataSourceFactory.build(environment.metrics(), "attribution-seeder");

        // Read in the seeds file and write things
        logger.info("Seeding consent entries.");

        try (final Connection connection = dataSource.getConnection();
             DSLContext context = DSL.using(connection, this.settings)) {

            // Like other seed commands, we truncate our one table first
            // since we only have the one table, this seems reasonable. See also DBUtils.truncateAllTables()
            context.truncateTable("consent").execute();

            readLines(Path.of(resource.getPath()), context);
        }

        logger.info("Consent entries seeded.");
    }

    private void readLines(Path filePath, DSLContext context) throws IOException {
        try (Stream<String> lines = Files.lines(filePath)) {
            lines
                    .skip(1)                                         // skip header row
                    .map(l -> l.split(","))
                    .forEach(w -> {
                        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
                        final ConsentEntity ce = new ConsentEntity();
                        ce.setId(UUID.fromString(w[0]));
                        ce.setMbi(w[1]);
                        ce.setHicn(w[2]);
                        ce.setEffectiveDate(LocalDate.parse(w[3]));
                        ce.setCreatedAt(now);
                        ce.setUpdatedAt(now);
                        ce.setPolicyCode(w[4]);
                        ce.setPurposeCode(w[5]);
                        ce.setLoincCode(w[6]);
                        ce.setScopeCode(w[7]);
                        ConsentRecord record = context.newRecord(CONSENT, ce);
                        context.executeInsert(record);
                    });
        }
    }
}
