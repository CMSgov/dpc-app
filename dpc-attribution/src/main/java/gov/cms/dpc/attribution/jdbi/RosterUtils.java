package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.dao.tables.records.AttributionsRecord;
import gov.cms.dpc.attribution.dao.tables.records.PatientsRecord;
import gov.cms.dpc.attribution.dao.tables.records.ProvidersRecord;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.util.UUID;

import static gov.cms.dpc.attribution.dao.tables.Patients.PATIENTS;
import static gov.cms.dpc.attribution.dao.tables.Providers.PROVIDERS;

public class RosterUtils {

    /**
     * Helper method for adding a roster {@link Bundle} to the RDBMS backend
     *
     * @param attributionBundle - Roster {@link Bundle} to add/update
     * @param ctx               - {@link DSLContext} DB context to utilize
     * @param creationTimestamp - {@link Timestamp} of when the attribution relationships were updated
     */
    public static void handleAttributionBundle(Bundle attributionBundle, DSLContext ctx, Timestamp creationTimestamp) {

        // Insert the provider , patients, and the attribution relationships
        final Practitioner provider = (Practitioner) attributionBundle.getEntryFirstRep().getResource();

        final ProviderEntity providerEntity = ProviderEntity.fromFHIR(provider);
        providerEntity.setProviderID(UUID.randomUUID());

//            logger.info("Adding provider {}", providerEntity.getProviderNPI());

        final ProvidersRecord providerRecord = ctx.newRecord(PROVIDERS, providerEntity);
        providerRecord.setId(UUID.randomUUID());
        new ProviderRecordUpserter(ctx, providerRecord).upsert();

        attributionBundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter((resource -> resource.getResourceType() == ResourceType.Patient))
                .map(patient -> PatientEntity.fromFHIR((Patient) patient))
                .forEach(patientEntity -> RosterUtils.createUpdateAttributionRelationship(ctx, patientEntity, providerRecord, creationTimestamp));
    }

    private static void createUpdateAttributionRelationship(DSLContext ctx, PatientEntity patientEntity, ProvidersRecord providerRecord, Timestamp creationTimestamp) {
        // Create a new record from the patient entity
        patientEntity.setPatientID(UUID.randomUUID());
        final PatientRecordUpserter patientRecordUpserter = new PatientRecordUpserter(ctx, ctx.newRecord(PATIENTS, patientEntity));
        final PatientsRecord patient = patientRecordUpserter.upsert();

        // Manually create the attribution relationship because JOOQ doesn't understand JPA ManyToOne relationships
        final AttributionsRecord attr = new AttributionsRecord();
        attr.setProviderId(providerRecord.getId());
        attr.setPatientId(patient.getId());
        attr.setCreatedAt(creationTimestamp);
        ctx.executeInsert(attr);
    }
}
