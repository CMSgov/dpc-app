package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.converters.AbstractEntityConversionTest;
import gov.cms.dpc.fhir.converters.FHIRConverter;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrganizationEntityTest extends AbstractEntityConversionTest {

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
}
