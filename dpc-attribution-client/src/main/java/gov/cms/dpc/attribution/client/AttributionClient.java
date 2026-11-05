package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.gclient.IQuery;
import com.google.re2j.Pattern;
import gov.cms.dpc.attribution.client.search.PatientSearch;
import gov.cms.dpc.attribution.client.search.RosterSearch;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.r4.FHIRExtractors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Client for the attribution service's R4 endpoints.
 *
 * <p>Searches and bulk operations return R4 {@link Bundle}s. Create operations return
 * {@link MethodOutcome}s.  Operation implementations wrap submitted resources in whatever FHIR
 * resource makes sense and deletes return nothing.  They either work or throw an exception.
 */
public class AttributionClient {

    private static final String GROUP_RESOURCE_TYPE = "Group";
    private static final String SUBMIT_OPERATION = "submit";

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z0-9]+://.*$");
    private final IGenericClient client;

    public AttributionClient(IGenericClient client) {
        this.client = Objects.requireNonNull(client);
    }

    /**
     * Search for an {@link Organization}
     *
     * @param identifier optional FHIR identifier token.  Can be a comma separated list, or null.
     *                   Null lists all organizations.
     */
    public Bundle searchOrganizations(String identifier) {
        Map<String, List<String>> searchParams = new HashMap<>();
        if (identifier != null) {
            searchParams.put("identifier", Collections.singletonList(identifier));
        }
        return searchResource(Organization.class)
            .whereMap(searchParams)
            .execute();
    }

    /**
     * Read an {@link Organization}
     */
    public Organization getOrganization(UUID id) {
        return readResource(id, Organization.class);
    }

    /**
     * Submit an {@link Organization}
     */
    public MethodOutcome submitOrganization(Bundle bundle) {
        final Parameters parameters = new Parameters();
        parameters.addParameter().setName("resource").setResource(bundle);

        return this.client
            .operation()
            .onType(Organization.class)
            .named(SUBMIT_OPERATION)
            .withParameters(parameters)
            .returnMethodOutcome()
            .encodedJson()
            .execute();
    }

    /**
     * Overwrite an {@link Organization}
     */
    public Organization updateOrganization(UUID id, Organization organization) {
        return updateResource(id, organization, Organization.class);
    }

    /**
     * Delete an {@link Organization}
     */
    public void deleteOrganization(UUID id) {
        deleteResource(id, "Organization");
    }

    /**
     * Search for a {@link Practitioner} by their ID
     *
     * @param organizationId required organization ID
     * @param practitionerId required practitioner resource ID
     */
    public Bundle searchPractitioners(UUID organizationId, UUID practitionerId) {
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("organization", Collections.singletonList(organizationId.toString()));
        searchParams.put("_id", Collections.singletonList(practitionerId.toString()));

        return searchResource(Practitioner.class)
            .whereMap(searchParams)
            .execute();
    }

    /**
     * Search for a {@link Practitioner} by their NPI
     *
     * @param organizationId required organization ID
     * @param providerNpi required provider NPI
     */
    public Bundle searchPractitioners(UUID organizationId, String providerNpi) {
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("organization", Collections.singletonList(organizationId.toString()));

        final var request = searchResource(Practitioner.class);

        if (providerNpi != null && !providerNpi.isEmpty()) {
            searchParams.put("identifier", Collections.singletonList(providerNpi));
        }

        return request
            .whereMap(searchParams)
            .execute();
    }

    /**
     * Read a {@link Practitioner}
     */
    public Practitioner getPractitioner(UUID id) {
        return readResource(id, Practitioner.class);
    }

    /**
     * Create a {@link Practitioner}
     */
    public MethodOutcome createPractitioner(Practitioner practitioner) {
        return createResource(practitioner);
    }

    /**
     * Submit a {@link Bundle} of {@link Practitioner}s
     */
    public Bundle submitPractitioners(Bundle bundle) {
        final Parameters params = new Parameters();
        params.addParameter().setResource(bundle);
        return this.client
            .operation()
            .onType(Practitioner.class)
            .named(SUBMIT_OPERATION)
            .withParameters(params)
            .returnResourceType(Bundle.class)
            .encodedJson()
            .execute();
    }

    /**
     * Overwrite a {@link Practitioner}
     */
    public Practitioner updatePractitioner(UUID id, Practitioner practitioner) {
        return updateResource(id, practitioner, Practitioner.class);
    }

    /**
     * Delete a {@link Practitioner}
     */
    public void deletePractitioner(UUID id) {
        deleteResource(id, "Practitioner");
    }

    /**
     * Search for a {@link Patient}
     */
    public Bundle searchPatients(PatientSearch query) {
        Objects.requireNonNull(query.organizationId(), "Organization ID is required");
        if (query.count() != null && query.count() < 0) {
            throw new IllegalArgumentException("Parameter _count must be >= 0");
        }
        if (query.offset() != null && query.offset() < 0) {
            throw new IllegalArgumentException("Parameter _offset must be >= 0");
        }

        var request = searchResource(Patient.class)
            .where(Patient.ORGANIZATION.hasId(query.organizationId().toString()));

        if (query.patientMbi() != null && !query.patientMbi().isEmpty()) {
            final String expandedMBI;
            if (IDENTIFIER_PATTERN.matcher(query.patientMbi()).matches()) {
                expandedMBI = query.patientMbi();
            } else {
                expandedMBI = String.format("%s|%s", DPCIdentifierSystem.MBI.getSystem(), query.patientMbi());
            }
            final Identifier identifier = FHIRExtractors.parseIDFromQueryParam(expandedMBI);
            request = request.where(Patient.IDENTIFIER.exactly()
                .systemAndIdentifier(identifier.getSystem(), identifier.getValue()));
        }
        if (query.patientId() != null) {
            request = request.where(new TokenClientParam("_id").exactly().code(query.patientId().toString()));
        }
        if (query.summary() != null) {
            request.summaryMode(SummaryEnum.fromCode(query.summary()));
        }
        if (query.offset() != null) {
            request.offset(query.offset());
        }

        // Count=0 forces summary
        if (query.count() != null) {
            request.count(query.count());
            if (query.count() == 0) {
                request.summaryMode(SummaryEnum.COUNT);
            }
        }
        return request.execute();
    }

    /**
     * Read a {@link Patient}
     */
    public Patient getPatient(UUID id) {
        return readResource(id, Patient.class);
    }

    /**
     * Create a {@link Patient}
     */
    public MethodOutcome createPatient(Patient patient) {
        return createResource(patient);
    }

    /**
     * Submit a {@link Bundle} of {@link Patient}s
     */
    public Bundle submitPatients(Bundle bundle) {
        final Parameters params = new Parameters();
        params.addParameter().setResource(bundle);
        return this.client
            .operation()
            .onType(Patient.class)
            .named(SUBMIT_OPERATION)
            .withParameters(params)
            .returnResourceType(Bundle.class)
            .encodedJson()
            .execute();
    }

    /**
     * Overwrite a {@link Patient}
     */
    public Patient updatePatient(UUID id, Patient patient) {
        return updateResource(id, patient, Patient.class);
    }

    /**
     * Delete a {@link Patient}
     */
    public void deletePatient(UUID id) {
        deleteResource(id, "Patient");
    }

    /**
     * Search for a {@link Group}
     */
    public Bundle searchRosters(RosterSearch query) {
        final Map<String, List<String>> queryParams = new HashMap<>();
        if (query.groupId() != null) {
            queryParams.put("_id", Collections.singletonList(query.groupId().toString()));
        }
        if (query.providerNpiCharacteristic() != null) {
            queryParams.put(Group.SP_CHARACTERISTIC_VALUE, Collections.singletonList(query.providerNpiCharacteristic()));
        }
        if (query.patientReference() != null) {
            queryParams.put("member", Collections.singletonList(query.patientReference()));
        }

        return searchResource(Group.class)
            .whereMap(queryParams)
            .withTag(DPCIdentifierSystem.DPC.getSystem(), query.organizationId().toString())
            .execute();
    }

    /**
     * Read a {@link Group}
     */
    public Group getRoster(UUID id) {
        return readResource(id, Group.class);
    }

    /**
     * Create a {@link Group}
     */
    public MethodOutcome createRoster(Group group) {
        return createResource(group);
    }

    /**
     * Overwrite a {@link Group}
     */
    public Group replaceRoster(UUID id, Group group) {
        return updateResource(id, group, Group.class);
    }

    /**
     * Delete a {@link Group}
     */
    public void deleteRoster(UUID id) {
        deleteResource(id, GROUP_RESOURCE_TYPE);
    }

    /**
     * Add {@link Patient}s to an existing {@link Group}.
     *
     * @param group {@link Group} containing the {@link Patient}s to be added
     */
    public Group addRosterMembers(UUID id, Group group) {
        return executeGroupOperation(id, group, "add");
    }

    /**
     * Remove {@link Patient}s from an existing {@link Group}.
     *
     * @param group {@link Group} containing the {@link Patient}s to be removed
     */
    public Group removeRosterMembers(UUID id, Group group) {
        return executeGroupOperation(id, group, "remove");
    }

    /**
     * Return a {@link Bundle} of {@link Patient}s in the {@link Group}.  Only the MBIs will
     * be filled in.
     */
    public Bundle getAttributedPatients(UUID id, boolean activeOnly) {
        final Parameters parameters = new Parameters();
        parameters.addParameter().setValue(new BooleanType(activeOnly)).setName("active");

        return this.client
            .operation()
            .onInstance(new IdType(GROUP_RESOURCE_TYPE, id.toString()))
            .named("patients")
            .withParameters(parameters)
            .returnResourceType(Bundle.class)
            .useHttpGet()
            .encodedJson()
            .execute();
    }

    private Group executeGroupOperation(UUID rosterID, Group groupUpdate, String operationName) {
        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(groupUpdate);
        return this.client
            .operation()
            .onInstance(new IdType(GROUP_RESOURCE_TYPE, rosterID.toString()))
            .named(operationName)
            .withParameters(parameters)
            .returnResourceType(Group.class)
            .encodedJson()
            .execute();
    }

    /**
     * Returns the attribution health endpoint's response.
     * Any exceptions get thrown to the caller.
     *
     * @throws IllegalStateException if the response is not a boolean
     */
    public boolean checkHealth() {
        final String health = getTextResponse("_healthy").trim();
        if (!Set.of("true", "false").contains(health)) {
            throw new IllegalStateException("Attribution service returned an invalid health response");
        }
        return Boolean.parseBoolean(health);
    }

    /**
     * Returns the version of dpc-attribution
     */
    public String getVersion() {
        return getTextResponse("version");
    }

    private String getTextResponse(String path) {
        final var response = this.client
            .rawHttpRequest()
            .get(path)
            .accept("text/plain")
            .execute();
        try (InputStream input = response.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read attribution response for " + path, e);
        }
    }

    // Search doesn't execute so callers can add their own filters first.
    private IQuery<Bundle> searchResource(Class<? extends Resource> resourceClass) {
        return this.client
            .search()
            .forResource(resourceClass)
            .encodedJson()
            .returnBundle(Bundle.class);
    }

    private <T extends Resource> T readResource(UUID id, Class<T> resourceClass) {
        return this.client
            .read()
            .resource(resourceClass)
            .withId(id.toString())
            .encodedJson()
            .execute();
    }

    private MethodOutcome createResource(Resource resource) {
        return this.client
            .create()
            .resource(resource)
            .encodedJson()
            .execute();
    }

    private <T extends Resource> T updateResource(UUID id, T resource, Class<T> resourceClass) {
        final MethodOutcome outcome = this.client
            .update()
            .resource(resource)
            .withId(id.toString())
            .encodedJson()
            .execute();

        final T updatedResource = resourceClass.cast(outcome.getResource());
        if (updatedResource == null) {
            throw new IllegalStateException("Attribution service returned no " + resourceClass.getSimpleName() + " after update");
        }
        return updatedResource;
    }

    private void deleteResource(UUID id, String resourceType) {
        this.client
            .delete()
            .resourceById(new IdType(resourceType, id.toString()))
            .encodedJson()
            .execute();
    }
}
