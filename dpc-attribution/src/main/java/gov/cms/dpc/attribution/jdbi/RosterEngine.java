package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.dao.tables.records.AttributionsRecord;
import gov.cms.dpc.attribution.dao.tables.records.PatientsRecord;
import gov.cms.dpc.attribution.dao.tables.records.ProvidersRecord;
import gov.cms.dpc.attribution.exceptions.AttributionException;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.FHIRExtractors;
import io.dropwizard.db.ManagedDataSource;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.jooq.*;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static gov.cms.dpc.attribution.dao.tables.Attributions.ATTRIBUTIONS;
import static gov.cms.dpc.attribution.dao.tables.Patients.PATIENTS;
import static gov.cms.dpc.attribution.dao.tables.Providers.PROVIDERS;
import static org.jooq.impl.DSL.values;

public class RosterEngine implements AttributionEngine {

    private static final Logger logger = LoggerFactory.getLogger(RosterEngine.class);
    private static final String CONNECTION_ERROR = "Unable to open connection to database";

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
        try (final DSLContext context = DSL.using(this.dataSource.getConnection(), this.settings)) {
            if (!context.fetchExists(context.selectOne().from(PROVIDERS).where(PROVIDERS.PROVIDER_ID.eq(providerNPI)))) {
                return Optional.empty();
            }
            final List<String> beneficiaryIDs = generateAttributionTableQuery(context, providerNPI)
                    .fetch().getValues(PATIENTS.BENEFICIARY_ID);

            return Optional.of(beneficiaryIDs);
        } catch (SQLException e) {
            throw new AttributionException(CONNECTION_ERROR, e);
        }
    }

    @Override
    public List<String> checkUnattributed(Group attributionGroup) {

        final String providerNPI = FHIRExtractors.getProviderNPIFromGroup(attributionGroup);
        try (final DSLContext context = DSL.using(this.dataSource.getConnection(), this.settings)) {

            @SuppressWarnings("unchecked") final Table<Record> tempTable = context
                    .select()
                    .from(values(attributionGroup
                            .getMember()
                            .stream()
                            .map(FHIRExtractors::getPatientMPIFromGroup)
                            .map(DSL::row).toArray(Row1[]::new))).asTable("v", "id");
            // Create a values table to help the patient IDs we're looking for

            // Patients attributed to the given provider
            final SelectConditionStep<Record1<String>> attributionTable = generateAttributionTableQuery(context, providerNPI);

            //  Field references, with type information
            final Field<String> beneIDReference = attributionTable.field("BENEFICIARY_ID", String.class);
            final Field<String> tempTableIDReference = tempTable.field("id", String.class);

            return context
                    .select()
                    .from(tempTable)
                    .leftOuterJoin(attributionTable)
                    .on(tempTableIDReference.eq(beneIDReference))
                    .where(beneIDReference.isNull())
                    .fetch().getValues(tempTableIDReference);
        } catch (SQLException e) {
            throw new AttributionException(CONNECTION_ERROR, e);
        }
    }

    @Override
    public void addAttributionRelationship(Practitioner provider, Patient patient) {

        try (final DSLContext context = DSL.using(this.dataSource.getConnection(), this.settings)) {
            context.transaction(config -> {
                final DSLContext ctx = DSL.using(config);

                final PatientRecordUpserter patientUpserter = new PatientRecordUpserter(ctx, ctx.newRecord(PATIENTS, patient));
                final ProviderRecordUpserter providerUpserter = new ProviderRecordUpserter(ctx, ctx.newRecord(PROVIDERS, provider));

                final PatientsRecord patientRecord = patientUpserter.upsert();
                final ProvidersRecord providerRecord = providerUpserter.upsert();

                // Manually create the attribution relationship because JOOQ doesn't understand JPA ManyToOne relationships
                final AttributionsRecord attr = new AttributionsRecord();
                attr.setProviderId(providerRecord.getId());
                attr.setPatientId(patientRecord.getId());
                attr.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

                final int updated = ctx.executeInsert(attr);
                if (updated != 1) {
                    throw new IllegalStateException("Attribution table was not modified");
                }
            });
        } catch (SQLException e) {
            throw new AttributionException(CONNECTION_ERROR, e);
        }
    }

    @Override
    public void addAttributionRelationships(Bundle attributionBundle) {
        try (final DSLContext context = DSL.using(this.dataSource.getConnection(), this.settings)) {
            context.transaction(config -> {

                final DSLContext ctx = DSL.using(config);
                RosterUtils.submitAttributionBundle(attributionBundle, ctx, OffsetDateTime.now(ZoneOffset.UTC));
            });
        } catch (SQLException e) {
            throw new AttributionException(CONNECTION_ERROR, e);
        }
    }

    @Override
    public void removeAttributionRelationship(Practitioner provider, Patient patient) {

        // We have to manually delete everything, until JOOQ supports DELETE CASCADE
        // https://github.com/jOOQ/jOOQ/issues/7367
        // We'll do it all in a single transaction

        // Fetch the relationships, then delete all the things
        try (final DSLContext context = DSL.using(this.dataSource.getConnection(), this.settings)) {
            context.transaction(config -> {

                final DSLContext ctx = DSL.using(config);
                final String providerNPI = FHIRExtractors.getProviderNPI(provider);
                final String patientMPI = FHIRExtractors.getPatientMPI(patient);
                final Result<AttributionsRecord> attributionsRecords = ctx.selectFrom(ATTRIBUTIONS
                        .join(PROVIDERS).on(ATTRIBUTIONS.PROVIDER_ID.eq(PROVIDERS.ID))
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
            throw new AttributionException(CONNECTION_ERROR, e);
        }
    }

    @Override
    public boolean isAttributed(Practitioner provider, Patient patient) {
        try (final DSLContext context = DSL.using(this.dataSource.getConnection(), this.settings)) {
            return context.fetchExists(context.selectOne()
                    .from(ATTRIBUTIONS)
                    .join(PATIENTS).on(PATIENTS.ID.eq(ATTRIBUTIONS.PATIENT_ID))
                    .join(PROVIDERS).on(PROVIDERS.ID.eq(ATTRIBUTIONS.PROVIDER_ID))
                    .where(PATIENTS.BENEFICIARY_ID
                            .eq(FHIRExtractors.getPatientMPI(patient))
                            .and(PROVIDERS.PROVIDER_ID
                                    .eq(FHIRExtractors.getProviderNPI(provider)))));
        } catch (SQLException e) {
            throw new AttributionException(CONNECTION_ERROR, e);
        }
    }

    @Override
    public void assertHealthy() {
        try (final DSLContext context = DSL.using(this.dataSource.getConnection(), this.settings)) {
            context.selectOne()
                    .from(ATTRIBUTIONS)
                    .fetchOptional();
        } catch (SQLException e) {
            throw new AttributionException(CONNECTION_ERROR, e);
        }
    }

    /**
     * Generate the SQL JOIN between the {@link gov.cms.dpc.attribution.dao.tables.Providers} table and the {@link gov.cms.dpc.attribution.dao.tables.Patients} table.
     * This JOIN returns all the patients associated to the givne provider, using the {@link gov.cms.dpc.attribution.dao.tables.Attributions} table.
     *
     * @param ctx        - {@link DSLContext} JOOQ context to use
     * @param providerID - {@link String} provider ID (NPI)
     * @return - table select resource
     */
    private static SelectConditionStep<Record1<String>> generateAttributionTableQuery(DSLContext ctx, String providerID) {
        return ctx.select(PATIENTS.BENEFICIARY_ID)
                .from(PATIENTS)
                .join(ATTRIBUTIONS).on(ATTRIBUTIONS.PATIENT_ID.eq(PATIENTS.ID))
                .join(PROVIDERS).on(ATTRIBUTIONS.PROVIDER_ID.eq(PROVIDERS.ID))
                .where(PROVIDERS.PROVIDER_ID.eq(providerID));
    }
}
