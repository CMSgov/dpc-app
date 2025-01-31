package gov.cms.dpc.attribution.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.dao.tables.Organizations;
import gov.cms.dpc.attribution.dao.tables.Patients;
import gov.cms.dpc.attribution.dao.tables.Providers;
import gov.cms.dpc.attribution.dao.tables.records.OrganizationsRecord;
import gov.cms.dpc.attribution.dao.tables.records.PatientsRecord;
import gov.cms.dpc.attribution.dao.tables.records.ProvidersRecord;
import gov.cms.dpc.attribution.jdbi.RosterUtils;
import gov.cms.dpc.attribution.utils.DBUtils;
import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.utils.SeedProcessor;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import io.dropwizard.core.Application;
import io.dropwizard.core.cli.EnvironmentCommand;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.core.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.hl7.fhir.dstu3.model.*;
import org.jooq.DSLContext;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class SeedCommand extends EnvironmentCommand<DPCAttributionConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(SeedCommand.class);
    private static final String CSV = "test_associations.csv";
    private static final String ORGANIZATION_BUNDLE = "organization_bundle.json";
    private static final String PROVIDER_BUNDLE = "provider_bundle.json";
    private static final String PATIENT_BUNDLE = "patient_bundle.json";
    private static final UUID ORGANIZATION_ID = UUID.fromString("46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0");

    private final Settings settings;

    public SeedCommand(Application<DPCAttributionConfiguration> application) {
        super(application, "seed", "Seed the attribution roster");
        this.settings = new Settings().withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED);
    }

    @Override
    public void configure(Subparser subparser) {
        subparser
                .addArgument("-t", "--timestamp")
                .dest("timestamp")
                .type(String.class)
                .required(false)
                .help("Custom timestamp to use when adding attributed relationships.");

        // Preserve config override capabilities
        super.configure(subparser);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, DPCAttributionConfiguration configuration) throws Exception {
        // Get the db factory
        final PooledDataSourceFactory dataSourceFactory = configuration.getDatabase();
        final ManagedDataSource dataSource = dataSourceFactory.build(environment.metrics(), "attribution-seeder");

        final OffsetDateTime creationTimestamp = generateTimestamp(namespace);

        // Read in the seeds file and write things
        logger.info("Seeding attributions at time {}", creationTimestamp.toLocalDateTime());

        try (final Connection connection = dataSource.getConnection();
             DSLContext context = DSL.using(connection, this.settings)) {

            // Truncate everything
            DBUtils.truncateAllTables(context, "public");

            final FhirContext ctx = FhirContext.forDstu3();
            final IParser parser = ctx.newJsonParser();
            final FHIREntityConverter converter = FHIREntityConverter.initialize();
            // Start with the Organizations
            seedOrganizationBundle(converter, context, parser);

            // Providers next
            seedProviderBundle(converter, context, parser, ORGANIZATION_ID);

            // Add the patients, saving the references
            final Map<String, Reference> patientReferences = seedPatientBundle(converter, context, parser, ORGANIZATION_ID);

            // Get the test attribution seeds
            seedAttributions(context, ORGANIZATION_ID, creationTimestamp, patientReferences);

            logger.info("Finished loading seeds");
        }
    }

    private void seedOrganizationBundle(FHIREntityConverter converter, DSLContext context, IParser parser) throws IOException {
        try (final InputStream orgBundleStream = SeedCommand.class.getClassLoader().getResourceAsStream(ORGANIZATION_BUNDLE)) {
            if (orgBundleStream == null) {
                throw new MissingResourceException("Can not find seeds file", this.getClass().getName(), CSV);
            }
            final Bundle bundle = parser.parseResource(Bundle.class, orgBundleStream);
            final List<OrganizationEntity> organizationEntities = BundleParser.parse(Organization.class,
                    bundle,
                    org -> converter.fromFHIR(OrganizationEntity.class, org), ORGANIZATION_ID);

            organizationEntities
                    .stream()
                    .map(entity -> organizationEntityToRecord(context, entity))
                    .forEach(context::executeInsert);
        }
    }

    private void seedProviderBundle(FHIREntityConverter converter, DSLContext context, IParser parser, UUID organizationID) throws IOException {
        try (final InputStream providerBundleStream = SeedCommand.class.getClassLoader().getResourceAsStream(PROVIDER_BUNDLE)) {
            final Parameters parameters = parser.parseResource(Parameters.class, providerBundleStream);
            final Bundle providerBundle = (Bundle) parameters.getParameterFirstRep().getResource();
            final List<ProviderEntity> providers = BundleParser.parse(Practitioner.class, providerBundle, provider -> converter.fromFHIR(ProviderEntity.class, provider), organizationID);

            providers
                    .stream()
                    .map(entity -> providersEntityToRecord(context, entity))
                    .forEach(context::executeInsert);
        }
    }

    private void seedAttributions(DSLContext context, UUID organizationID, OffsetDateTime creationTimestamp, Map<String, Reference> patientReferences) throws IOException {
        try (InputStream resource = SeedCommand.class.getClassLoader().getResourceAsStream(CSV)) {
            if (resource == null) {
                throw new MissingResourceException("Can not find seeds file", this.getClass().getName(), CSV);
            }
            SeedProcessor
                    .extractProviderMap(resource)
                    .entrySet()
                    .stream()
                    .map(entry -> SeedProcessor.generateAttributionGroup(entry, organizationID, patientReferences))
                    .forEach(bundle -> RosterUtils.submitAttributionGroup(bundle, context, organizationID, creationTimestamp));
        }
    }

    private Map<String, Reference> seedPatientBundle(FHIREntityConverter converter, DSLContext context, IParser parser, UUID organizationID) throws IOException {
        try (final InputStream providerBundleStream = SeedCommand.class.getClassLoader().getResourceAsStream(PATIENT_BUNDLE)) {
            final Parameters parameters = parser.parseResource(Parameters.class, providerBundleStream);
            final Bundle patientBundle = (Bundle) parameters.getParameterFirstRep().getResource();
            final List<PatientEntity> patients = BundleParser.parse(Patient.class, patientBundle, patient -> converter.fromFHIR(PatientEntity.class, patient), organizationID);

            Map<String, Reference> patientReferences = new HashMap<>();

            patients
                    .stream()
                    // Add the managing organization
                    .peek(entity -> {
                        final OrganizationEntity organization = new OrganizationEntity();
                        organization.setId(organizationID);
                        entity.setOrganization(organization);
                    })
                    .map(entity -> patientEntityToRecord(context, entity))
                    .peek(context::executeInsert)
                    .forEach(patientRecord -> {
                        final Reference ref = new Reference(new IdType("Patient", patientRecord.getId().toString()));
                        patientReferences.put(patientRecord.getBeneficiaryId(), ref);
                    });

            return patientReferences;
        }
    }

    private static OffsetDateTime generateTimestamp(Namespace namespace) {
        final String timestamp = namespace.getString("timestamp");
        if (timestamp == null) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        return OffsetDateTime.parse(timestamp.trim());
    }

    private static OrganizationsRecord organizationEntityToRecord(DSLContext context, OrganizationEntity entity) {
        // We have to manually map the embedded fields
        final OrganizationsRecord newRecord = context.newRecord(Organizations.ORGANIZATIONS, entity);
        newRecord.setIdSystem(entity.getOrganizationID().getSystem().ordinal());
        newRecord.setIdValue(entity.getOrganizationID().getValue());

        final AddressEntity address = entity.getOrganizationAddress();
        newRecord.setAddressType(address.getType().ordinal());
        newRecord.setAddressUse(address.getUse().ordinal());
        newRecord.setLine1(address.getLine1());
        newRecord.setLine2(address.getLine2());
        newRecord.setCity(address.getCity());
        newRecord.setDistrict(address.getDistrict());
        newRecord.setState(address.getState());
        newRecord.setPostalCode(address.getPostalCode());
        newRecord.setCountry(address.getCountry());
        return newRecord;
    }

    private static ProvidersRecord providersEntityToRecord(DSLContext context, ProviderEntity entity) {
        final ProvidersRecord newRecord = context.newRecord(Providers.PROVIDERS, entity);
        newRecord.setOrganizationId(entity.getOrganization().getId());
        final OffsetDateTime created = OffsetDateTime.now(ZoneOffset.UTC);
        newRecord.setCreatedAt(created);
        newRecord.setUpdatedAt(created);
        newRecord.setId(UUID.randomUUID());

        return newRecord;
    }

    private static PatientsRecord patientEntityToRecord(DSLContext context, PatientEntity entity) {
        // Generate a temporary ID
        final PatientsRecord newRecord = context.newRecord(Patients.PATIENTS, entity);
        newRecord.setOrganizationId(entity.getOrganization().getId());
        final OffsetDateTime created = OffsetDateTime.now(ZoneOffset.UTC);
        newRecord.setCreatedAt(created);
        newRecord.setUpdatedAt(created);
        newRecord.setGender(entity.getGender().ordinal());
        newRecord.setId(UUID.randomUUID());

        return newRecord;
    }
}
