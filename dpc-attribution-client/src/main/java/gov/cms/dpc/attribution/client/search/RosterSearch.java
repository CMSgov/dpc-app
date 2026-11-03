package gov.cms.dpc.attribution.client.search;

import java.util.UUID;

/**
 * Attribution roster search filters.
 *
 * @param groupId optional Group resource ID
 * @param organizationId required organization ID; the client supplies the organization tag
 * @param providerNPI optional FHIR characteristic-value composite token
 * @param patientReference optional patientId, may be in the form of "Patient/id" or just "id"
 */
public record RosterSearch(UUID groupId, UUID organizationId, String providerNPI,
						   String patientReference) {
}
