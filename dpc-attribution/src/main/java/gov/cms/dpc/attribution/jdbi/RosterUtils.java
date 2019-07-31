package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.dao.tables.Attributions;
import gov.cms.dpc.attribution.dao.tables.records.AttributionsRecord;
import gov.cms.dpc.attribution.dao.tables.records.PatientsRecord;
import gov.cms.dpc.attribution.dao.tables.records.ProvidersRecord;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.*;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

import static gov.cms.dpc.attribution.dao.tables.Patients.PATIENTS;
import static gov.cms.dpc.attribution.dao.tables.Providers.PROVIDERS;

public class RosterUtils {

    private static final Logger logger = LoggerFactory.getLogger(RosterUtils.class);

    private RosterUtils() {
        // Not used
    }

    /**
     * Helper method for adding a roster {@link Bundle} to the RDBMS backend
     * When submitting the bundle, the values in the provided {@link Bundle} are merged with any existing values in the database.
     *
     * @param attributionBundle - Roster {@link Bundle} to add/update
     * @param ctx               - {@link DSLContext} DB context to utilize
     * @param organizationID    - {@link UUID} organization ID to associate to patient
     * @param creationTimestamp - {@link Timestamp} of when the attribution relationships were updated
     */
    public static void submitAttributionBundle(Bundle attributionBundle, DSLContext ctx, UUID organizationID, OffsetDateTime creationTimestamp) {

        // Insert the provider, patient, and attribution relationships
        final Practitioner provider = (Practitioner) attributionBundle.getEntryFirstRep().getResource();

        final Meta meta = new Meta();
        meta.addTag(DPCIdentifierSystem.DPC.getSystem(), organizationID.toString(), "Organization ID");
        provider.setMeta(meta);

        final ProviderEntity providerEntity = ProviderEntity.fromFHIR(provider);
        if (providerEntity.getProviderID() == null) {
            providerEntity.setProviderID(UUID.randomUUID());
        }

        logger.debug("Adding provider {}", providerEntity.getProviderNPI());
        final ProvidersRecord providerRecord = ctx.newRecord(PROVIDERS, providerEntity);
        // Get the Org ID, because Jooq can't do it on its own
        providerRecord.setOrganizationId(providerEntity.getOrganization().getId());
        // Upsert the record and get the new ID
        providerRecord.setId(new ProviderRecordUpserter(ctx, providerRecord).upsert().getId());

        attributionBundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType() == ResourceType.Patient)
                .map(resource -> (Patient) resource)
                .peek(patient -> patient.setManagingOrganization(new Reference("Organization/" + organizationID.toString())))
                .map(PatientEntity::fromFHIR)
                .forEach(patientEntity -> RosterUtils.createUpdateAttributionRelationship(ctx, patientEntity, providerRecord, creationTimestamp));
    }

    private static void createUpdateAttributionRelationship(DSLContext ctx, PatientEntity patientEntity, ProvidersRecord providerRecord, OffsetDateTime creationTimestamp) {
        // Create a new record from the patient entity
        if (patientEntity.getPatientID() == null) {
            patientEntity.setPatientID(UUID.randomUUID());
        }

        final PatientsRecord patientsRecord = ctx.newRecord(PATIENTS, patientEntity);
        patientsRecord.setOrganizationId(patientEntity.getOrganization().getId());
        final PatientRecordUpserter patientRecordUpserter = new PatientRecordUpserter(ctx, patientsRecord);
        final PatientsRecord patient = patientRecordUpserter.upsert();

        // If the attribution relationship already exists, ignore it.

        final boolean attributionExist = false;
//        final boolean attributionExist = ctx.fetchExists(Attributions.ATTRIBUTIONS,
//                Attributions.ATTRIBUTIONS.PROVIDER_ID
//                        .eq(providerRecord.getId())
//                        .and(Attributions.ATTRIBUTIONS.PATIENT_ID.eq(patient.getId())));

        final String beneficiaryID = patientEntity.getBeneficiaryID();
        final String providerNPI = providerRecord.getProviderId();

        if (!attributionExist) {
            logger.debug("Attributing patient {} to provider {}.", beneficiaryID, providerNPI);
            // Manually create the attribution relationship because JOOQ doesn't understand JPA ManyToOne relationships
            final AttributionsRecord attr = new AttributionsRecord();
//            attr.setProviderId(providerRecord.getId());
            attr.setPatientId(patient.getId());
            attr.setCreatedAt(creationTimestamp);
            ctx.executeInsert(attr);
        } else {
            logger.debug("Attribution relationship already exists between patient {} and provider {}", beneficiaryID, providerRecord.getProviderId());
        }
    }
}
