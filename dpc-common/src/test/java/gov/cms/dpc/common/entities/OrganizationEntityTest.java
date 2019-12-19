package gov.cms.dpc.common.entities;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(BufferedLoggerHandler.class)
class OrganizationEntityTest {

    @Test
    void testSimpleSerialDeserial() {

        final FHIREntityConverter converter = FHIREntityConverter.initialize();

        final InputStream inputStream = OrganizationEntityTest.class.getClassLoader().getResourceAsStream("test_org.json");
        final Organization org = (Organization) FhirContext.forDstu3().newJsonParser().parseResource(inputStream);
        final OrganizationEntity entity = converter.fromFHIR(OrganizationEntity.class, org);
        assertEquals(org.getName(), entity.getOrganizationName(), "Name should match");
    }
}
