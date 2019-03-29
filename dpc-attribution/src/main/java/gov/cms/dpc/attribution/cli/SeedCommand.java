package gov.cms.dpc.attribution.cli;

import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.dao.tables.Patients;
import gov.cms.dpc.attribution.dao.tables.Providers;
import gov.cms.dpc.attribution.dao.tables.records.AttributionsRecord;
import gov.cms.dpc.attribution.dao.tables.records.PatientsRecord;
import gov.cms.dpc.attribution.dao.tables.records.ProvidersRecord;
import gov.cms.dpc.common.entities.AttributionRelationship;
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
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
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

        final OffsetDateTime creationTimestamp = OffsetDateTime.now();

        // Read in the seeds file and write things
        logger.info("Seeding attributions at time {}");

        try (DSLContext context = DSL.using(dataSource.getConnection(), this.settings)) {

            // Truncate everything
            context.truncate(Patients.PATIENTS).cascade().execute();
            context.truncate(Providers.PROVIDERS).cascade().execute();

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

                        final ProvidersRecord pr = context.newRecord(Providers.PROVIDERS, providerEntity);
                        pr.setId(UUID.randomUUID());
                        context.executeInsert(pr);

                        bundle
                                .getEntry()
                                .stream()
                                .map(Bundle.BundleEntryComponent::getResource)
                                .filter((resource -> resource.getResourceType() == ResourceType.Patient))
                                .map(patient -> PatientEntity.fromFHIR((Patient) patient))
                                .forEach(patientEntity -> {
                                    // Create a new record from the patient entity
                                    patientEntity.setPatientID(UUID.randomUUID());
                                    final PatientsRecord patient = context.newRecord(Patients.PATIENTS, patientEntity);
                                    context.executeInsert(patient);

                                    // Manually create the attribution relationship because JOOQ doesn't understand JPA ManyToOne relationships
                                    attr.setCreatedAt(Timestamp.from(creationTimestamp.toInstant()));
                                    attr.setProviderId(pr.getId());
                                    attr.setPatientId(patient.getId());
                                    context.executeInsert(attr);
                                });

                                });

                    });
            logger.info("Finished loading seeds");
        }
    }
}
