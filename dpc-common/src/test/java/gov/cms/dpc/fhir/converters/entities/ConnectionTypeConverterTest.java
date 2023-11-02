package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.Coding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void fromFHIR(){
        EndpointEntity.ConnectionType convertedConnectionType = converter.fromFHIR(fhirEntityConverter, coding);
        assertEquals(connectionType.getCode(), convertedConnectionType.getCode());
        assertEquals(connectionType.getSystem(), convertedConnectionType.getSystem());
    }

    @Test
    void toFHIR(){
        Coding convertedCoding = converter.toFHIR(fhirEntityConverter, connectionType);
        assertEquals(coding.getCode(), convertedCoding.getCode());
        assertEquals(coding.getSystem(), convertedCoding.getSystem());
    }
    @Test
    void getFHIRResource() {
        assertEquals(Coding.class, converter.getFHIRResource());
    }

    @Test
    void getJavaClass() {
        assertEquals(EndpointEntity.ConnectionType.class, converter.getJavaClass());
    }
}
