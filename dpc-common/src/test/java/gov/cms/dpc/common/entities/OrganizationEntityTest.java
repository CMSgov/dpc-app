
package gov.cms.dpc.common.entities;

import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import gov.cms.dpc.fhir.converters.AbstractEntityConversionTest;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import gov.cms.dpc.fhir.converters.FHIRConverter;
import gov.cms.dpc.fhir.converters.entities.OrganizationEntityConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class OrganizationEntityTest extends AbstractEntityConversionTest{

	@Test
    @Disabled // Disabled until DPC-935
    void testSimpleRoundTrip() {
        final OrganizationEntity entity = new OrganizationEntity();
        entity.setId(UUID.randomUUID());
        entity.setOrganizationName("Test Organization");
        entity.setOrganizationID(new OrganizationEntity.OrganizationID(DPCIdentifierSystem.NPPES, "1234"));

        final Organization organization = super.converter.toFHIR(Organization.class, entity);
        assertEquals(entity, super.converter.fromFHIR(OrganizationEntity.class, organization), "Should be equal");
    }


    @Override
    protected List<FHIRConverter<?, ?>> registerConverters() {
        return Collections.singletonList(new OrganizationEntityConverter());
    }

	@Test
	public void testGettersAndSetters() {
		OrganizationEntity org = new OrganizationEntity();
		UUID id = UUID.randomUUID();
		OrganizationEntity.OrganizationID organizationID = new OrganizationEntity.OrganizationID(DPCIdentifierSystem.NPPES,"1234");
		String orgName = "CMS";
		AddressEntity orgAddress = new AddressEntity();
		List<ContactEntity> contacts = new ArrayList<>();
		List<EndpointEntity> endpointEntities = new ArrayList<>();
		List<ProviderEntity> providerEntities = new ArrayList<>();
		List<PatientEntity> patientEntities = new ArrayList<>();
		List<RosterEntity> rosters = new ArrayList<>();

		org.setId(id);
		org.setOrganizationID(organizationID);
		org.setOrganizationName(orgName);
		org.setOrganizationAddress(orgAddress);
		org.setContacts(contacts);
		org.setEndpoints(endpointEntities);
		org.setProviders(providerEntities);
		org.setPatients(patientEntities);
		org.setRosters(rosters);

		assertEquals(id, org.getId());
		assertEquals(organizationID, org.getOrganizationID());
		assertEquals(orgName, org.getOrganizationName());
		assertEquals(orgAddress, org.getOrganizationAddress());
		assertEquals(contacts, org.getContacts());
		assertEquals(endpointEntities, org.getEndpoints());
		assertEquals(providerEntities, org.getProviders());
		assertEquals(patientEntities, org.getPatients());
		assertEquals(rosters, org.getRosters());
	
	}

	@Test
	public void testUpdate() {
		OrganizationEntity o1 = new OrganizationEntity();
		OrganizationEntity o2 = new OrganizationEntity();
		o1.setOrganizationName("Test-Entity");
		o2.update(o1);

		assertEquals(o1.getOrganizationName(), o2.getOrganizationName());
	}

	@Test
	public void testOrganizationIDGettersAndSetters() {
		OrganizationEntity.OrganizationID orgId = new OrganizationEntity.OrganizationID();
		DPCIdentifierSystem system = DPCIdentifierSystem.NPPES;
		String val = "1234";

		orgId.setSystem(system);
		orgId.setValue(val);

		assertEquals(system, orgId.getSystem());
		assertEquals(val, orgId.getValue());
	}

	@Test
	public void testOrganizationIDToFHIR() {
		OrganizationEntity.OrganizationID orgId = new OrganizationEntity.OrganizationID(DPCIdentifierSystem.NPPES,"1234");
		Identifier fhirID = orgId.toFHIR();

		assertEquals(DPCIdentifierSystem.NPPES.getSystem(), fhirID.getSystem());
		assertEquals("1234", fhirID.getValue());
	}
}
