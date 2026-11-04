package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.attribution.client.search.PatientSearch;
import gov.cms.dpc.attribution.client.search.RosterSearch;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * HAPI-backed implementation of {@link AttributionClient}.
 */
public class AttributionClientImpl implements AttributionClient {

    private final IGenericClient client;

    public AttributionClientImpl(IGenericClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public Bundle searchOrganizations(String identifier) {
        Map<String, List<String>> searchParams = new HashMap<>();
        if (identifier != null) {
            searchParams.put("identifier", Collections.singletonList(identifier));
        }
        return this.client
            .search()
            .forResource(Organization.class)
            .whereMap(searchParams)
            .encodedJson()
            .returnBundle(Bundle.class)
            .execute();
    }

    @Override
    public Organization getOrganization(UUID id) {
        return this.client
            .read()
            .resource(Organization.class)
            .withId(id.toString())
            .encodedJson()
            .execute();
    }

    @Override
    public MethodOutcome submitOrganization(Bundle bundle) {
        final Parameters parameters = new Parameters();
        parameters.addParameter().setName("resource").setResource(bundle);

        return this.client
            .operation()
            .onType(Organization.class)
            .named("submit")
            .withParameters(parameters)
            .returnMethodOutcome()
            .encodedJson()
            .execute();
    }

    @Override
    public Organization updateOrganization(UUID id, Organization organization) {
        MethodOutcome outcome = this.client
            .update()
            .resource(organization)
            .withId(id.toString())
            .encodedJson()
            .execute();

        final Organization resource = (Organization) outcome.getResource();
        if (resource == null) {
            throw new IllegalStateException("Attribution service returned no Organization after update");
        }
        return resource;
    }

    @Override
    public void deleteOrganization(UUID id) {
        this.client
            .delete()
            .resourceById(new IdType("Organization", id.toString()))
            .encodedJson()
            .execute();
    }

    @Override
    public Bundle searchPractitioners(UUID organizationId, UUID practitionerId) {
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("organization", Collections.singletonList(organizationId.toString()));
        searchParams.put("_id", Collections.singletonList(practitionerId.toString()));

        return this.client
            .search()
            .forResource(Practitioner.class)
            .encodedJson()
            .returnBundle(Bundle.class)
            .whereMap(searchParams)
            .execute();
    }

    @Override
    public Bundle searchPractitioners(UUID organizationId, String providerNpi) {
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("organization", Collections.singletonList(organizationId.toString()));

        final var request = this.client
            .search()
            .forResource(Practitioner.class)
            .encodedJson()
            .returnBundle(Bundle.class);

        if (providerNpi != null && !providerNpi.isEmpty()) {
            searchParams.put("identifier", Collections.singletonList(providerNpi));
        }

        return request
            .whereMap(searchParams)
            .execute();
    }

    @Override
    public Practitioner getPractitioner(UUID id) {
        return this.client
            .read()
            .resource(Practitioner.class)
            .withId(id.toString())
            .encodedJson()
            .execute();
    }

    @Override
    public MethodOutcome createPractitioner(Practitioner practitioner) {
        return this.client
            .create()
            .resource(practitioner)
            .encodedJson()
            .execute();
    }

    @Override
    public Bundle submitPractitioners(Bundle bundle) {
        final Parameters params = new Parameters();
        params.addParameter().setResource(bundle);
        return this.client
            .operation()
            .onType(Practitioner.class)
            .named("submit")
            .withParameters(params)
            .returnResourceType(Bundle.class)
            .encodedJson()
            .execute();
    }

    @Override
    public Practitioner updatePractitioner(UUID id, Practitioner practitioner) {
        MethodOutcome outcome = this.client
            .update()
            .resource(practitioner)
            .withId(id.toString())
            .encodedJson()
            .execute();

        final Practitioner resource = (Practitioner) outcome.getResource();
        if (resource == null) {
            throw new IllegalStateException("Attribution service returned no Practitioner after update");
        }
        return resource;
    }

    @Override
    public void deletePractitioner(UUID id) {
        this.client
            .delete()
            .resourceById(new IdType("Practitioner", id.toString()))
            .encodedJson()
            .execute();
    }

    @Override
    public Bundle searchPatients(PatientSearch query) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Patient getPatient(UUID id) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public MethodOutcome createPatient(Patient patient) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Bundle submitPatients(Bundle bundle) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Patient updatePatient(UUID id, Patient patient) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public void deletePatient(UUID id) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Bundle searchRosters(RosterSearch query) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Group getRoster(UUID id) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public MethodOutcome createRoster(Group group) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Group replaceRoster(UUID id, Group group) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public void deleteRoster(UUID id) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Group addRosterMembers(UUID id, Group group) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Group removeRosterMembers(UUID id, Group group) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public Bundle getAttributedPatients(UUID id, boolean activeOnly) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public boolean checkHealth() {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public String getVersion() {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }
}
