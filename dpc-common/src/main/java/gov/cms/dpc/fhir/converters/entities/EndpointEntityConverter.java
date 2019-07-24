package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.EndpointEntity;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;


public class EndpointEntityConverter {

    private EndpointEntityConverter() {
        // Not used
    }

    public static Endpoint convert(EndpointEntity entity) {
        final Endpoint endpoint = new Endpoint();
        endpoint.setId(new IdType("Endpoint", entity.getId().toString()));
        endpoint.setName(entity.getName());
        endpoint.setAddress(endpoint.getAddress());

        endpoint.setManagingOrganization(new Reference(new IdType("Organization", entity.getOrganization().getId().toString())));
        endpoint.setStatus(entity.getStatus());
        endpoint.setConnectionType(ConnectionTypeConverter.convert(entity.getConnectionType()));

        return endpoint;
    }
}
