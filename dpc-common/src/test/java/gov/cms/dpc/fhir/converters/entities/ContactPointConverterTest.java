package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.ContactPointEntity;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ContactPointConverterTest {
    ContactPointConverter converter = new ContactPointConverter();
    FHIREntityConverter fhirEntityConverter = FHIREntityConverter.initialize();
    static String EMAIL = "bob@example.com";
    ContactPoint contactPoint;
    ContactPointEntity contactPointEntity;
    @BeforeEach
    void buildEntities() {
        contactPoint = new ContactPoint();
        contactPoint.setSystem(ContactPoint.ContactPointSystem.EMAIL);
        contactPoint.setUse(ContactPoint.ContactPointUse.HOME);
        contactPoint.setValue(EMAIL);

        contactPointEntity = new ContactPointEntity();
        contactPointEntity.setSystem(ContactPoint.ContactPointSystem.EMAIL);
        contactPointEntity.setUse(ContactPoint.ContactPointUse.HOME);
        contactPointEntity.setValue(EMAIL);
    }

    @Test
    void fromFHIR() {
        ContactPointEntity convertedEntity = converter.fromFHIR(fhirEntityConverter, contactPoint);
        assertEquals(contactPoint.getSystem(), convertedEntity.getSystem());
        assertEquals(contactPoint.getUse(), convertedEntity.getUse());
        assertEquals(EMAIL, convertedEntity.getValue());
        assertNull(convertedEntity.getRank());
    }

    @Test
    void fromFHIR_WithRank() {
        contactPoint.setRank(4);
        ContactPointEntity convertedEntity = converter.fromFHIR(fhirEntityConverter, contactPoint);
        assertEquals(4, convertedEntity.getRank());
    }


    @Test
    void toFHIR() {
        ContactPoint convertedEntity = converter.toFHIR(fhirEntityConverter, contactPointEntity);
        assertEquals(contactPointEntity.getSystem(), convertedEntity.getSystem());
        assertEquals(contactPointEntity.getUse(), convertedEntity.getUse());
        assertEquals(EMAIL, convertedEntity.getValue());
        assertEquals(0, convertedEntity.getRank());
    }

    @Test
    void toFHIR_WithRank() {
        contactPointEntity.setRank(4);
        ContactPoint convertedEntity = converter.toFHIR(fhirEntityConverter, contactPointEntity);
        assertEquals(4, convertedEntity.getRank());
    }


    @Test
    void getFHIRResource() {
        assertEquals(ContactPoint.class, converter.getFHIRResource());
    }

    @Test
    void getJavaClass() {
        assertEquals(ContactPointEntity.class, converter.getJavaClass());
    }
}
