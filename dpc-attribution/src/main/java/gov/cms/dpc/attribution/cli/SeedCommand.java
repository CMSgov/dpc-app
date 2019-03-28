package gov.cms.dpc.attribution.cli;

import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.dao.tables.Attributions;
import gov.cms.dpc.attribution.dao.tables.Patients;
import gov.cms.dpc.attribution.dao.tables.Providers;
import gov.cms.dpc.attribution.dao.tables.records.AttributionsRecord;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.common.utils.SeedProcessor;
import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.jooq.DSLContext;
import org.jooq.TableRecord;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.UUID;

public class SeedCommand extends EnvironmentCommand<DPCAttributionConfiguration> {

    private static Logger logger = LoggerFactory.getLogger(SeedCommand.class);
    private static final String CSV = "test_associations.csv";

    private final SeedProcessor seedProcessor;
    private final Settings settings;


    public SeedCommand(Application<DPCAttributionConfiguration> application) {
        super(application, "seed", "Seed the attribution roster");
        // Get the test seeds
        final InputStream resource = SeedCommand.class.getClassLoader().getResourceAsStream(CSV);
        if (resource == null) {
            throw new MissingResourceException("Can not find seeds file", this.getClass().getName(), CSV);
        }
        this.seedProcessor = new SeedProcessor(resource);

        this.settings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, DPCAttributionConfiguration configuration) throws Exception {
        // Get the db factory
        final PooledDataSourceFactory dataSourceFactory = configuration.getDatabase();
        final ManagedDataSource dataSource = dataSourceFactory.build(environment.metrics(), "attribution-seeder");

        // Read in the seeds file and write things
        logger.info("Seeding attributions");

        try (DSLContext context = DSL.using(dataSource.getConnection(), this.settings)) {

            // Truncate everything
            context.truncate(Attributions.ATTRIBUTIONS);

            this.seedProcessor
                    .extractProviderMap()
                    .entrySet()
                    .stream()
                    .map(this.seedProcessor::generateRosterBundle)
                    .forEach(bundle -> {
                        // Insert the provider , patients, and the attribution relationships
                        final Practitioner provider = (Practitioner) bundle.getEntryFirstRep().getResource();

                        final ProviderEntity providerEntity = ProviderEntity.fromFHIR(provider);
                        providerEntity.setProviderID(UUID.randomUUID());

                        logger.info("Adding provider {}", providerEntity.getProviderNPI());

                        // Create a list of records to insert into the database
                        // We don't care about the type, so we'll just use a wildcard
                        List<TableRecord<?>> insertList = new ArrayList<>();

                        insertList.add(context.newRecord(Providers.PROVIDERS, providerEntity));

                        bundle
                                .getEntry()
                                .stream()
                                .map(Bundle.BundleEntryComponent::getResource)
                                .filter((resource -> resource.getResourceType() == ResourceType.Patient))
                                .map(patient -> PatientEntity.fromFHIR((Patient) patient))
                                .forEach(patientEntity -> {
                                    // Create a new record from the patient entity
                                    insertList.add(context.newRecord(Patients.PATIENTS, patientEntity));

                                    // Manually create a new Attribution record
                                    final AttributionsRecord attr = new AttributionsRecord();
                                    attr.setProviderId(providerEntity.getProviderID());
                                    attr.setPatientId(patientEntity.getPatientID());
                                    attr.setCreatedAt(Timestamp.from(Instant.now()));
                                    insertList.add(attr);
                                });
                        // Insert everything in a single transaction
                        context.batchInsert(insertList);

                    });
            logger.info("Finished loading seeds");
        }
    }
}
