package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.EndpointEntity;
import org.hl7.fhir.dstu3.model.Coding;

public class ConnectionTypeConverter {

    private ConnectionTypeConverter() {
        // Not used
    }

    public static Coding convert(EndpointEntity.ConnectionType entity) {
        final Coding coding = new Coding();
        coding.setSystem(entity.getSystem());
        coding.setCode(entity.getCode());

        return coding;
    }
}
