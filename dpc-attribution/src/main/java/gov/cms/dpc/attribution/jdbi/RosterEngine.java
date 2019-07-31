package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.dao.tables.records.AttributionsRecord;
import gov.cms.dpc.attribution.dao.tables.records.PatientsRecord;
import gov.cms.dpc.attribution.dao.tables.records.ProvidersRecord;
import gov.cms.dpc.attribution.exceptions.AttributionException;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.FHIRExtractors;
import io.dropwizard.db.ManagedDataSource;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static gov.cms.dpc.attribution.dao.tables.Attributions.ATTRIBUTIONS;
import static gov.cms.dpc.attribution.dao.tables.Patients.PATIENTS;
import static gov.cms.dpc.attribution.dao.tables.Providers.PROVIDERS;

public class RosterEngine implements AttributionEngine {

    static final String CONNECTION_MESSAGE = "Unable to open connection to database";
    private static final Logger logger = LoggerFactory.getLogger(RosterEngine.class);

    private final ManagedDataSource dataSource;
    private final Settings settings;

    @Inject
    RosterEngine(ManagedDataSource dataSource, Settings settings) {
        this.dataSource = dataSource;
        this.settings = settings;
    }


    @Override
    public Optional<List<String>> getAttributedPatientIDs(Practitioner provider) {

        final String providerNPI = FHIRExtractors.getProviderNPI(provider);

        try (final Connection connection = this.dataSource.getConnection(); final DSLContext context = DSL.using(connection, this.settings)) {
            if (!context.fetchExists(context.selectOne().from(PROVIDERS).where(PROVIDERS.PROVIDER_ID.eq(providerNPI)))) {
                return Optional.empty();
            }
            final List<String> beneficiaryIDs = context.select()
                    .from(PROVIDERS)
//                    .join(ATTRIBUTIONS).on(ATTRIBUTIONS.PROVIDER_ID.eq(PROVIDERS.ID))
                    .join(PATIENTS).on(ATTRIBUTIONS.PATIENT_ID.eq(PATIENTS.ID))
                    .where(PROVIDERS.PROVIDER_ID.eq(providerNPI))
                    .fetch().getValues(PATIENTS.BENEFICIARY_ID);

            return Optional.of(beneficiaryIDs);
        } catch (SQLException e) {
            throw new AttributionException("Unable to open connection to database", e);
        }
    }

    @Override
    public void addAttributionRelationship(Practitioner provider, Patient patient) {

        try (final Connection connection = this.dataSource.getConnection();
             final DSLContext context = DSL.using(connection, this.settings)) {
            context.transaction(config -> {
                final DSLContext ctx = DSL.using(config);

                final PatientRecordUpserter patientUpserter = new PatientRecordUpserter(ctx, ctx.newRecord(PATIENTS, patient));
                final ProviderRecordUpserter providerUpserter = new ProviderRecordUpserter(ctx, ctx.newRecord(PROVIDERS, provider));

                final PatientsRecord patientRecord = patientUpserter.upsert();
                final ProvidersRecord providerRecord = providerUpserter.upsert();

                // Manually create the attribution relationship because JOOQ doesn't understand JPA ManyToOne relationships
                final AttributionsRecord attr = new AttributionsRecord();
//                attr.setProviderId(providerRecord.getId());
                attr.setPatientId(patientRecord.getId());
                attr.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

                final int updated = ctx.executeInsert(attr);
                if (updated != 1) {
                    throw new IllegalStateException("Attribution table was not modified");
                }
            });
        } catch (SQLException e) {
            throw new AttributionException("Unable to open connection to database", e);
        }
    }

    @Override
    public void addAttributionRelationships(Bundle attributionBundle, UUID organizationID) {
        try (final Connection connection = this.dataSource.getConnection();
             final DSLContext context = DSL.using(connection, this.settings)) {
            context.transaction(config -> {

                final DSLContext ctx = DSL.using(config);
                RosterUtils.submitAttributionBundle(attributionBundle, ctx, organizationID, OffsetDateTime.now(ZoneOffset.UTC));
            });
        } catch (SQLException e) {
            throw new AttributionException("Unable to open connection to database", e);
        }
    }

    @Override
    public void removeAttributionRelationship(Practitioner provider, Patient patient) {

        // We have to manually delete everything, until JOOQ supports DELETE CASCADE
        // https://github.com/jOOQ/jOOQ/issues/7367
        // We'll do it all in a single transaction

        // Fetch the relationships, then delete all the things
        try (final Connection connection = this.dataSource.getConnection();
             final DSLContext context = DSL.using(connection, this.settings)) {
            context.transaction(config -> {

                final DSLContext ctx = DSL.using(config);
                final String providerNPI = FHIRExtractors.getProviderNPI(provider);
                final String patientMPI = FHIRExtractors.getPatientMPI(patient);
                final Result<AttributionsRecord> attributionsRecords = ctx.selectFrom(ATTRIBUTIONS
//                        .join(PROVIDERS).on(ATTRIBUTIONS.PROVIDER_ID.eq(PROVIDERS.ID))
                        .join(PATIENTS).on(ATTRIBUTIONS.PATIENT_ID.eq(PATIENTS.ID)))
                        .where(PROVIDERS.PROVIDER_ID.eq(providerNPI).and(PATIENTS.BENEFICIARY_ID.eq(patientMPI)))
                        .fetchInto(ATTRIBUTIONS);

                if (attributionsRecords.isEmpty()) {
                    logger.warn("Cannot find attribution relationship between {} and {}.", providerNPI, patientMPI);
                }

                // Just get the first one, because we know they're unique
                final AttributionsRecord attributionsRecord = attributionsRecords.get(0);

                // Remove all the records
                final int removed = ctx.deleteFrom(ATTRIBUTIONS).where(ATTRIBUTIONS.ID.eq(attributionsRecord.getId())).execute();

                if (removed != 1) {
                    throw new IllegalStateException("Failure removing attribution relationship. Row leftover");
                }
            });
        } catch (SQLException e) {
            throw new AttributionException("Unable to open connection to database", e);
        }
    }

    @Override
    public boolean isAttributed(Practitioner provider, Patient patient) {
        try (final Connection connection = this.dataSource.getConnection();
             final DSLContext context = DSL.using(connection, this.settings)) {
            return context.fetchExists(context.selectOne()
                    .from(ATTRIBUTIONS)
                    .join(PATIENTS).on(PATIENTS.ID.eq(ATTRIBUTIONS.PATIENT_ID))
//                    .join(PROVIDERS).on(PROVIDERS.ID.eq(ATTRIBUTIONS.PROVIDER_ID))
                    .where(PATIENTS.BENEFICIARY_ID
                            .eq(FHIRExtractors.getPatientMPI(patient))
                            .and(PROVIDERS.PROVIDER_ID
                                    .eq(FHIRExtractors.getProviderNPI(provider)))));
        } catch (SQLException e) {
            throw new AttributionException("Unable to open connection to database", e);
        }
    }

    @Override
    public void assertHealthy() {
        try (final Connection connection = this.dataSource.getConnection();
             final DSLContext context = DSL.using(connection, this.settings)) {
            context.selectOne()
                    .from(ATTRIBUTIONS)
                    .fetchOptional();
        } catch (SQLException e) {
            throw new AttributionException(CONNECTION_MESSAGE, e);
        }
    }
}
