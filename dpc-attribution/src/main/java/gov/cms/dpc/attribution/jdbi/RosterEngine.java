package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.dao.tables.records.AttributionsRecord;
import gov.cms.dpc.attribution.dao.tables.records.PatientsRecord;
import gov.cms.dpc.attribution.dao.tables.records.ProvidersRecord;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.FHIRExtractors;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static gov.cms.dpc.attribution.dao.tables.Attributions.ATTRIBUTIONS;
import static gov.cms.dpc.attribution.dao.tables.Patients.PATIENTS;
import static gov.cms.dpc.attribution.dao.tables.Providers.PROVIDERS;

public class RosterEngine implements AttributionEngine {

    private static final Logger logger = LoggerFactory.getLogger(RosterEngine.class);

    private final DSLContext context;

    @Inject
    RosterEngine(DSLContext context) {
        this.context = context;
    }


    @Override
    public Optional<List<String>> getAttributedPatientIDs(Practitioner provider) {

        final String providerNPI = FHIRExtractors.getProviderNPI(provider);

        if (!context.fetchExists(context.selectOne().from(PROVIDERS).where(PROVIDERS.PROVIDER_ID.eq(providerNPI)))) {
            return Optional.empty();
        }
        final List<String> beneficiaryIDs = context.select()
                .from(PROVIDERS)
                .join(ATTRIBUTIONS).on(ATTRIBUTIONS.PROVIDER_ID.eq(PROVIDERS.ID))
                .join(PATIENTS).on((ATTRIBUTIONS.PATIENT_ID).eq(PATIENTS.ID))
                .where(PROVIDERS.PROVIDER_ID.eq(providerNPI))
                .fetch().getValues(PATIENTS.BENEFICIARY_ID);

        return Optional.of(beneficiaryIDs);
    }

    @Override
    public void addAttributionRelationship(Practitioner provider, Patient patient) {

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
            attr.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

            final int updated = ctx.executeInsert(attr);
            if (updated != 1) {
                throw new IllegalStateException("Attribution table was not modified");
            }
        });
    }

    @Override
    public void addAttributionRelationships(Bundle attributionBundle) {
        context.transaction(config -> {

            final DSLContext ctx = DSL.using(config);
            final Timestamp creationTimestamp = Timestamp.from(Instant.now());
            RosterUtils.handleAttributionBundle(attributionBundle, ctx, creationTimestamp);
        });
    }

    @Override
    public void removeAttributionRelationship(Practitioner provider, Patient patient) {

        // We have to manually delete everything, until JOOQ supports DELETE CASCADE
        // https://github.com/jOOQ/jOOQ/issues/7367
        // We'll do it all in a single transaction

        // Fetch the relationships, then delete all the things
        context.transaction(config -> {

            final DSLContext ctx = DSL.using(config);
            final String providerNPI = FHIRExtractors.getProviderNPI(provider);
            final String patientMPI = FHIRExtractors.getPatientMPI(patient);
            final Result<AttributionsRecord> attributionsRecords = ctx.selectFrom(ATTRIBUTIONS
                    .join(PROVIDERS).on(ATTRIBUTIONS.PROVIDER_ID.eq(PROVIDERS.ID))
                    .join(PATIENTS).on((ATTRIBUTIONS.PATIENT_ID).eq(PATIENTS.ID)))
                    .where(PROVIDERS.PROVIDER_ID.eq(providerNPI).and(PATIENTS.BENEFICIARY_ID.eq(patientMPI)))
                    .fetchInto(ATTRIBUTIONS);

            if (attributionsRecords.isEmpty()) {
                logger.warn("Cannot find attribution relationship between {} and {}.");
            }

            // Just get the first one, because we know they're unique
            final AttributionsRecord attributionsRecord = attributionsRecords.get(0);

            // Remove all the records
            int removed = 0;
            removed += ctx.deleteFrom(ATTRIBUTIONS).where(ATTRIBUTIONS.ID.eq(attributionsRecord.getId())).execute();
            removed += ctx.deleteFrom(PATIENTS).where(PATIENTS.ID.eq(attributionsRecord.getPatientId())).execute();
            removed += ctx.deleteFrom(PROVIDERS).where(PROVIDERS.ID.eq(attributionsRecord.getPatientId())).execute();

            if (removed != 3) {
                throw new IllegalStateException("Failure removing attribution relationship. Row leftover");
            }
        });
    }

    @Override
    public boolean isAttributed(Practitioner provider, Patient patient) {
//        return true;
        return context.fetchExists(context.selectOne()
                .from(ATTRIBUTIONS)
                .join(PATIENTS).on(PATIENTS.ID.eq(ATTRIBUTIONS.PATIENT_ID))
                .join(PROVIDERS).on(PROVIDERS.ID.eq(ATTRIBUTIONS.PROVIDER_ID))
                .where(PATIENTS.BENEFICIARY_ID
                        .eq(FHIRExtractors.getPatientMPI(patient))
                        .and(PROVIDERS.PROVIDER_ID
                                .eq(FHIRExtractors.getProviderNPI(provider)))));
    }
}
