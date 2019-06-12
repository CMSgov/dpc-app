package gov.cms.dpc.common.entities;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrganizationEntityTest {

    @Test
    void testSimpleSerialDeserial() {

        final InputStream inputStream = OrganizationEntityTest.class.getClassLoader().getResourceAsStream("test_org.json");
        final Organization org = (Organization) FhirContext.forDstu3().newJsonParser().parseResource(inputStream);
        final OrganizationEntity entity = new OrganizationEntity().fromFHIR(org);
        assertEquals(org.getName(), entity.getOrganizationName(), "Name should match");
    }
}
