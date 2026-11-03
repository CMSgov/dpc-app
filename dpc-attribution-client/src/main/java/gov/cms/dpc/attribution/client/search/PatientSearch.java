package gov.cms.dpc.attribution.client.search;

import java.util.UUID;

/**
 * Patient search filters.
 *
 * @param patientId optional patient resource ID
 * @param patientMbi optional MBI
 * @param organizationId required organization ID
 * @param count optional max Patients to return.  Null returns all.
 * @param offset optional offset.  Used when paging a large number of Patients.
 * @param summary optional set to "count" to return a summary Bundle
 */
public record PatientSearch(UUID patientId, String patientMbi, UUID organizationId,
							Integer count, Integer offset, String summary) {
}
