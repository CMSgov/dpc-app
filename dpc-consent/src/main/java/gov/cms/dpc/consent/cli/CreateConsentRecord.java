package gov.cms.dpc.consent.cli;


import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.consent.DPCConsentConfiguration;
import gov.cms.dpc.consent.dao.tables.records.ConsentRecord;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.jooq.DSLContext;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static gov.cms.dpc.consent.dao.tables.Consent.CONSENT;

/**
 * CreateConsentRecord adds a consent record for a specific beneficiary to the consent database. While most consent
 * records are recorded through our consent ETL process, we want to be ready to create single consent records on
 * demand where the custodian can be an external organization or DPC.
 * <p>
 * The command takes the following arguments:
 * <ul>
 * <li>-p, --patient - (required) a patient MBI (currently the BB bene_id) known to DPC</li>
 * <li>-i, --in - indicates this is in an optin event</li>
 * <li>-o, --out - indicates this is an optout event</li>
 * <li>-d, --date - (required) sets the effective date of the record</li>
 * <li>--org - (optional) if provided, must be an organization uuid known to DPC</li>
 * </ul>
 */
public class CreateConsentRecord extends ConsentCommand {

    private static final Logger logger = LoggerFactory.getLogger(CreateConsentRecord.class);

    private static final String IN_OR_OUT_ARG = "inOrOut";

    private final Settings settings;

    CreateConsentRecord() {
        super("create", "Create a new consent record");
        this.settings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {
        subparser.addArgument("-p", "--patient")
                .required(true)
                .dest("mbi")
                .help("MBI of patient in this record");

        subparser.addArgument("-d", "--date")
                .required(true)
                .dest("effective-date")
                .help("effective date of this record (e.g., 2019-11-20)");

        subparser.addArgument("--org")
                .required(false)
                .dest("org-uuid")
                .help("DPC UUID of an external org that originated this record");

        addInOrOutGroup(subparser);
    }

    private void addInOrOutGroup(Subparser subparser) {
        MutuallyExclusiveGroup group = subparser.addMutuallyExclusiveGroup();
        group
                .addArgument("-i", "--in")
                .dest(IN_OR_OUT_ARG)
                .action(Arguments.storeConst()).setConst(ConsentEntity.OPT_IN)
                .help("flag indicating this is an optin record; mutually exclusive with -o");
        group
                .addArgument("-o", "--out")
                .dest(IN_OR_OUT_ARG)
                .action(Arguments.storeConst()).setConst(ConsentEntity.OPT_OUT)
                .help("flag indicating this is an optout record; mutually exclusive with -i");

        group.required(true);
    }

    @Override
    protected void run(Bootstrap<DPCConsentConfiguration> bootstrap, Namespace namespace, DPCConsentConfiguration dpcConsentConfiguration) throws DataAccessException, SQLException  {
        final String mbi = namespace.getString("mbi");
        final LocalDate effectiveDate = LocalDate.parse(namespace.getString("effective-date"));
        final String inOrOut = namespace.getString(IN_OR_OUT_ARG);

        final String orgUuid = namespace.getString("org-uuid");
        if (orgUuid != null && !orgUuid.isBlank()) {
            UUID.fromString(orgUuid);
            // using UUID conversion to verify the UUID provided is valid; will throw IllegalArgumentException if invalid
        }

        // TODO verify mbi / org exist in DPC attribution

        logger.info(
                String.format("Creating %s consent entry for patient %s, custodian is %s, effective %s",
                        inOrOut, mbi, orgUuid == null ? "DPC" : orgUuid, effectiveDate));

        ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.empty(), Optional.of(mbi));
        ce.setEffectiveDate(effectiveDate);
        ce.setPolicyCode(inOrOut);

        if (orgUuid != null && !orgUuid.isBlank()) {
            ce.setCustodian(UUID.fromString(orgUuid));
        }

        saveEntity(bootstrap, dpcConsentConfiguration, ce);

        String custodian = ce.getCustodian() == null ? ce.getSourceCode() : ce.getCustodian().toString();
        logger.info(String.format("Created %s consent entry for patient %s, custodial entity is %s, effective %s",
                ce.getPolicyCode(), ce.getMbi(), custodian, ce.getEffectiveDate()));
    }

    private void saveEntity(Bootstrap<DPCConsentConfiguration> bootstrap, DPCConsentConfiguration dpcConsentConfiguration, ConsentEntity entity) throws DataAccessException, SQLException {
        final PooledDataSourceFactory dataSourceFactory = dpcConsentConfiguration.getConsentDatabase();
        final ManagedDataSource dataSource = dataSourceFactory.build(bootstrap.getMetricRegistry(), "consent-cli");

        try (final Connection connection = dataSource.getConnection();
             DSLContext context = DSL.using(connection, this.settings)) {

            ConsentRecord record = context.newRecord(CONSENT, entity);
            context.executeInsert(record);
        }
    }
}
