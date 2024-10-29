package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.ContactPointEntity;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Contact point conversion")
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
    @DisplayName("Convert contact point with attributes from FHIR 🥳")
    void fromFHIR() {
        ContactPointEntity convertedEntity = converter.fromFHIR(fhirEntityConverter, contactPoint);
        assertEquals(contactPoint.getSystem(), convertedEntity.getSystem());
        assertEquals(contactPoint.getUse(), convertedEntity.getUse());
        assertEquals(EMAIL, convertedEntity.getValue());
        assertNull(convertedEntity.getRank());
    }

    @Test
    @DisplayName("Convert contact point with rank from FHIR 🥳")
    void fromFHIR_WithRank() {
        contactPoint.setRank(4);
        ContactPointEntity convertedEntity = converter.fromFHIR(fhirEntityConverter, contactPoint);
        assertEquals(4, convertedEntity.getRank());
    }


    @Test
    @DisplayName("Convert contact point with attributes to FHIR 🥳")
    void toFHIR() {
        ContactPoint convertedEntity = converter.toFHIR(fhirEntityConverter, contactPointEntity);
        assertEquals(contactPointEntity.getSystem(), convertedEntity.getSystem());
        assertEquals(contactPointEntity.getUse(), convertedEntity.getUse());
        assertEquals(EMAIL, convertedEntity.getValue());
        assertEquals(0, convertedEntity.getRank());
    }

    @Test
    @DisplayName("Convert contact point with rank to FHIR 🥳")
    void toFHIR_WithRank() {
        contactPointEntity.setRank(4);
        ContactPoint convertedEntity = converter.toFHIR(fhirEntityConverter, contactPointEntity);
        assertEquals(4, convertedEntity.getRank());
    }


    @Test
    @DisplayName("Convert Contact Point Java class to FHIR 🥳")
    void getFHIRResource() {
        assertEquals(ContactPoint.class, converter.getFHIRResource());
    }

    @Test
    @DisplayName("Convert Contact Point Entity to Java class 🥳")
    void getJavaClass() {
        assertEquals(ContactPointEntity.class, converter.getJavaClass());
    }
}
