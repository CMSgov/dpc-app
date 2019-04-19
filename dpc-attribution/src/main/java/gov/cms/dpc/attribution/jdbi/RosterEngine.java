package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.dao.tables.records.AttributionsRecord;
import gov.cms.dpc.attribution.dao.tables.records.PatientsRecord;
import gov.cms.dpc.attribution.dao.tables.records.ProvidersRecord;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.FHIRExtractors;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static gov.cms.dpc.attribution.dao.tables.Attributions.ATTRIBUTIONS;
import static gov.cms.dpc.attribution.dao.tables.Patients.PATIENTS;
import static gov.cms.dpc.attribution.dao.tables.Providers.PROVIDERS;

public class RosterEngine implements AttributionEngine {

    private final DSLContext context;

    @Inject
    RosterEngine(DSLContext context) {
        this.context = context;
    }


    @Override
    public Optional<List<String>> getAttributedPatientIDs(Practitioner provider) {

        try {
            return Optional.of(context.select()
                    .from(PROVIDERS)
                    .join(ATTRIBUTIONS).on(ATTRIBUTIONS.PROVIDER_ID.eq(PROVIDERS.ID))
                    .join(PATIENTS).on((ATTRIBUTIONS.PATIENT_ID).eq(PATIENTS.ID))
                    .where(PROVIDERS.PROVIDER_ID.eq(FHIRExtractors.getProviderNPI(provider)))
                    .fetch().getValues(PATIENTS.BENEFICIARY_ID));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void addAttributionRelationship(Practitioner provider, Patient patient) {

        context.transaction(config -> {

            final DSLContext ctx = DSL.using(config);


            final PatientRecordUpserter patientUpserter = new PatientRecordUpserter(ctx, ctx.newRecord(PATIENTS, patient));
            final ProviderRecordUpserter providerUpserter = new ProviderRecordUpserter(ctx, ctx.newRecord(PROVIDERS, provider));

            final PatientsRecord patientRecord = patientUpserter.upsert();
            final ProvidersRecord providerRecord = providerUpserter.upsert();

//            ctx.insertInto(providerRecord.getTable())
//                    .set(providerRecord)
//                    .onConflict(PROVIDERS.PROVIDER_ID)
//                    .doUpdate()
//                    .set(providerRecord)
//                    .execute();

            // Manually create the attribution relationship because JOOQ doesn't understand JPA ManyToOne relationships
            final AttributionsRecord attr = new AttributionsRecord();
            attr.setProviderId(providerRecord.getId());
            attr.setPatientId(patientRecord.getId());
            attr.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

            ctx.executeInsert(attr);
        });
    }

    @Override
    public void addAttributionRelationships(Bundle attributionBundle) {
//        context.transaction(config -> {

//            final DSLContext ctx = DSL.using(config);
        final DSLContext ctx = context;


        final Timestamp creationTimestamp = Timestamp.from(Instant.now());
        // Insert the provider , patients, and the attribution relationships
        final Practitioner provider = (Practitioner) attributionBundle.getEntryFirstRep().getResource();

        final ProviderEntity providerEntity = ProviderEntity.fromFHIR(provider);
        providerEntity.setProviderID(UUID.randomUUID());

//            logger.info("Adding provider {}", providerEntity.getProviderNPI());

        final ProvidersRecord pr = ctx.newRecord(PROVIDERS, providerEntity);
        pr.setId(UUID.randomUUID());
        new ProviderRecordUpserter(ctx, pr).upsert();
//        this.upsertRecord(ctx, pr, Collections.emptyList(), PROVIDERS.PROVIDER_ID);

        attributionBundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter((resource -> resource.getResourceType() == ResourceType.Patient))
                .map(patient -> PatientEntity.fromFHIR((Patient) patient))
                .forEach(patientEntity -> {
                    // Create a new record from the patient entity
                    patientEntity.setPatientID(UUID.randomUUID());
                    final PatientRecordUpserter patientRecordUpserter = new PatientRecordUpserter(ctx, ctx.newRecord(PATIENTS, patientEntity));
                    final PatientsRecord patient = patientRecordUpserter.upsert();
//                    patient.setId(UUID.randomUUID());
//                    this.upsertRecord(ctx, patient, Collections.singletonList(PATIENTS.ID), PATIENTS.BENEFICIARY_ID);

                    // Manually create the attribution relationship because JOOQ doesn't understand JPA ManyToOne relationships
                    final AttributionsRecord attr = new AttributionsRecord();
                    attr.setProviderId(pr.getId());
                    attr.setPatientId(patient.getId());
                    attr.setCreatedAt(creationTimestamp);
                    ctx.executeInsert(attr);
                });
//        });
    }

    @Override
    public void removeAttributionRelationship(Practitioner provider, Patient patient) {
        throw new UnsupportedOperationException("Cannot remove with JOOQ");
        // Manually create the attribution relationship because JOOQ doesn't understand JPA ManyToOne relationships
//        final AttributionsRecord attr = new AttributionsRecord();
//        attr.setProviderId(UUID.fromString(FHIRExtractors.getProviderNPI(provider)));
//        attr.setPatientId(UUID.fromString(FHIRExtractors.getPatientMPI(patient)));
//        attr.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
//
//        context.executeInsert(attr);
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
