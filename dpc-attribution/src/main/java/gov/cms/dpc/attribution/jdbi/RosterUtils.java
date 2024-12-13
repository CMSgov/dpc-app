package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.dao.tables.records.AttributionsRecord;
import gov.cms.dpc.attribution.dao.tables.records.ProvidersRecord;
import gov.cms.dpc.attribution.dao.tables.records.RostersRecord;
import gov.cms.dpc.fhir.FHIRExtractors;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.IdType;
import org.jooq.DSLContext;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static gov.cms.dpc.attribution.dao.tables.Providers.PROVIDERS;

public class RosterUtils {

    private RosterUtils() {
        // Not used
    }

    public static void submitAttributionGroup(Group attributionGroup, DSLContext ctx, UUID organizationID, OffsetDateTime creationTimestamp) {
        // Insert the Roster and attribution relationships

        // Get the provider, by NPI
        final String providerNPI = FHIRExtractors.getAttributedNPI(attributionGroup);

        final ProvidersRecord providersRecord = ctx.selectFrom(PROVIDERS)
                .where(PROVIDERS.PROVIDER_ID.eq(providerNPI))
                .fetchOne();

        final RostersRecord roster = new RostersRecord();
        roster.setId(UUID.randomUUID());
        roster.setOrganizationId(organizationID);
        roster.setProviderId(providersRecord.getId());
        roster.setCreatedAt(creationTimestamp);
        ctx.executeInsert(roster);

        // Now, the attribution relationships
        attributionGroup
                .getMember()
                .stream()
                .map(Group.GroupMemberComponent::getEntity)
                .map(entity -> new IdType(entity.getReference()))
                .map(id -> {
                    final AttributionsRecord ar = new AttributionsRecord();
                    ar.setPeriodBegin(creationTimestamp);
                    ar.setPeriodEnd(creationTimestamp.plus(90, ChronoUnit.DAYS));
                    ar.setRosterId(roster.getId());
                    ar.setPatientId(UUID.fromString(id.getIdPart()));
                    return ar;
                })
                .forEach(ctx::executeInsert);
    }

}
