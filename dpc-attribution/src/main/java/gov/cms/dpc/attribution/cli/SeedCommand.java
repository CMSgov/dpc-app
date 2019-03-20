package gov.cms.dpc.attribution.cli;

import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.engine.TestSeeder;
import gov.cms.dpc.attribution.models.AttributionRelationship;
import gov.cms.dpc.attribution.models.PatientEntity;
import gov.cms.dpc.attribution.models.ProviderEntity;
import gov.cms.dpc.common.utils.SeedProcessor;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.MissingResourceException;
import java.util.UUID;

public class SeedCommand extends ConfiguredCommand<DPCAttributionConfiguration> {

    private static Logger logger = LoggerFactory.getLogger(SeedCommand.class);
    private static final String CSV = "test_associations.csv";

    private final SeedProcessor seedProcessor;


    public SeedCommand() {
        super("seed", "Seed the attribution roster");
        // Get the test seeds
        final InputStream resource = TestSeeder.class.getClassLoader().getResourceAsStream(CSV);
        if (resource == null) {
            throw new MissingResourceException("Can not find seeds file", this.getClass().getName(), CSV);
        }
        this.seedProcessor = new SeedProcessor(resource);
    }

    @Override
    protected void run(Bootstrap<DPCAttributionConfiguration> bootstrap, Namespace namespace, DPCAttributionConfiguration configuration) throws Exception {
        // Get the db factory
        final PooledDataSourceFactory dataSourceFactory = configuration.getDatabase();
        dataSourceFactory.asSingleConnectionPool();
        final ManagedDataSource dataSource = dataSourceFactory.build(new MetricRegistry(), "attribution-seeder");

        // Read in the seeds file and write things
        logger.info("Seeding attributions");

        // Truncate everything

        try (final Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.beginRequest();
            try (Statement truncateStatement = connection.createStatement()) {
                truncateStatement.execute("TRUNCATE TABLE PROVIDERS CASCADE ");
                truncateStatement.execute("TRUNCATE TABLE PATIENTS CASCADE ");
            }

            // TODO: This should be moved to a more robust SQL framework, which will be handled in DPC-169
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

                        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO providers (id, provider_id, first_name, last_name) VALUES (?, ?, ?, ?)")) {
                            statement.setObject(1, providerEntity.getProviderID());
                            statement.setObject(2, providerEntity.getProviderNPI());
                            statement.setString(3, providerEntity.getProviderFirstName());
                            statement.setString(4, providerEntity.getProviderLastName());
                            statement.execute();
                        } catch (SQLException e) {
                            throw new IllegalStateException(e);
                        }
                        bundle
                                .getEntry()
                                .stream()
                                .map(Bundle.BundleEntryComponent::getResource)
                                .filter((resource -> resource.getResourceType() == ResourceType.Patient))
                                .map(patient -> PatientEntity.fromFHIR((Patient) patient))
                                // Add the patient
                                .forEach(patientEntity -> {
                                    patientEntity.setPatientID(UUID.randomUUID());
                                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO patients (id, beneficiary_id, first_name, last_name, dob) VALUES (?, ?, ?, ?, ?)")) {
                                        statement.setObject(1, patientEntity.getPatientID());
                                        statement.setObject(2, patientEntity.getBeneficiaryID());
                                        statement.setString(3, patientEntity.getPatientFirstName());
                                        statement.setString(4, patientEntity.getPatientLastName());
                                        statement.setDate(5, Date.valueOf(patientEntity.getDob()));
                                        statement.execute();


                                    } catch (SQLException e) {
                                        throw new IllegalStateException(e);
                                    }

                                    // Add the relationship
                                    new AttributionRelationship(providerEntity, patientEntity, OffsetDateTime.now());
                                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO attributions (provider_id, patient_id, created_at) VALUES (?, ?, ?)")) {
//                                        statement.setObject(1, UUID.randomUUID());
                                        statement.setObject(1, providerEntity.getProviderID());
                                        statement.setObject(2, patientEntity.getPatientID());
                                        statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                                        statement.execute();
                                    } catch (SQLException e) {
                                        throw new IllegalStateException(e);
                                    }

                                });
                    });
            connection.commit();
            logger.info("Finished loading seeds");
            dataSource.stop();
        }
    }
}
