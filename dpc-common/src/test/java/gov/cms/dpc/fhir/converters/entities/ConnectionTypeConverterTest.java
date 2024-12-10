package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.Coding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Connection Type conversion")
public class ConnectionTypeConverterTest {
    ConnectionTypeConverter converter = new ConnectionTypeConverter();
    FHIREntityConverter fhirEntityConverter = FHIREntityConverter.initialize();
    EndpointEntity.ConnectionType connectionType;
    Coding coding;

    @BeforeEach
    void buildEntities() {
        String code = "Some Code";
        String system = "Some System";

        connectionType = new EndpointEntity.ConnectionType();
        connectionType.setCode(code);

        connectionType.setSystem(system);
        coding = new Coding(system, code, "");
    }

    @Test
@DisplayName("Convert connection type with attributes from FHIR 🥳")

    void fromFHIR(){
        EndpointEntity.ConnectionType convertedConnectionType = converter.fromFHIR(fhirEntityConverter, coding);
        assertEquals(connectionType.getCode(), convertedConnectionType.getCode());
        assertEquals(connectionType.getSystem(), convertedConnectionType.getSystem());
    }

    @Test
@DisplayName("Convert connection type with attributes to FHIR 🥳")

    void toFHIR(){
        Coding convertedCoding = converter.toFHIR(fhirEntityConverter, connectionType);
        assertEquals(coding.getCode(), convertedCoding.getCode());
        assertEquals(coding.getSystem(), convertedCoding.getSystem());
    }
    @Test
@DisplayName("Convert Coding Java class to FHIR resource 🥳")

    void getFHIRResource() {
        assertEquals(Coding.class, converter.getFHIRResource());
    }

    @Test
@DisplayName("Convert Connection Type FHIR resource to Java class 🥳")

    void getJavaClass() {
        assertEquals(EndpointEntity.ConnectionType.class, converter.getJavaClass());
    }
}
