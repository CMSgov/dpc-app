package gov.cms.dpc.fhir;

import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DPCResourceTypeTests {

    @ParameterizedTest
    @EnumSource(DPCResourceType.class)
    void testGetPath(DPCResourceType resourceType) {
        String resourceName = resourceType.toString();
        assertEquals(resourceName.toLowerCase(), resourceType.getPath());
        assertEquals(resourceType, DPCResourceType.fromCode(resourceName));
    }

    @Test
    void testUnknownCode() {
        FHIRException exception = assertThrows(FHIRException.class, () -> DPCResourceType.fromCode("foo"));
        assertEquals("Unknown resource type: foo", exception.getMessage());
    }

}
