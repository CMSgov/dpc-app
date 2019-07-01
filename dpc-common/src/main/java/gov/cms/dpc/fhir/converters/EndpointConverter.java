package gov.cms.dpc.fhir.converters;

import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.fhir.FHIRExtractors;
import org.hl7.fhir.dstu3.model.Endpoint;

import java.util.UUID;

public class EndpointConverter {

    private EndpointConverter() {
        // Not used
    }

    public static EndpointEntity convert(Endpoint resource) {
        final EndpointEntity entity = new EndpointEntity();

        if (resource.getId() == null) {
            entity.setId(UUID.randomUUID());
        } else {
            entity.setId(FHIRExtractors.getEntityUUID(resource.getId()));
        }

        entity.setName(resource.getName());
        entity.setAddress(resource.getAddress());
        entity.setStatus(resource.getStatus());

        final EndpointEntity.ConnectionType connectionType = new EndpointEntity.ConnectionType();
        connectionType.setSystem(resource.getConnectionType().getSystem());
        connectionType.setCode(resource.getConnectionType().getCode());
        entity.setConnectionType(connectionType);

        final OrganizationEntity org = new OrganizationEntity();
        if (resource.hasManagingOrganization()) {
            org.setId(FHIRExtractors.getEntityUUID(resource.getManagingOrganization().getReference()));
        }
        entity.setOrganization(org);

        return entity;
    }
}
