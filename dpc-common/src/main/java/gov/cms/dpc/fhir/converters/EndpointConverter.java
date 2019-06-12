package gov.cms.dpc.fhir.converters;

import gov.cms.dpc.common.entities.EndpointEntity;
import org.hl7.fhir.dstu3.model.Endpoint;

public class EndpointConverter {

    private EndpointConverter() {
        // Not used
    }

    public static EndpointEntity convert(Endpoint resource) {
        final EndpointEntity entity = new EndpointEntity();

        entity.setName(resource.getName());
        entity.setAddress(resource.getAddress());
        entity.setStatus(resource.getStatus());

        final EndpointEntity.ConnectionType connectionType = new EndpointEntity.ConnectionType();
        connectionType.setSystem(resource.getConnectionType().getSystem());
        connectionType.setCode(resource.getConnectionType().getCode());
        entity.setConnectionType(connectionType);

        return entity;
    }
}
