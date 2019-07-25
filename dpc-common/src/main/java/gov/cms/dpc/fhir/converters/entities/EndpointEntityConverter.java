package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.fhir.validations.profiles.EndpointProfile;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Reference;


public class EndpointEntityConverter {

    private EndpointEntityConverter() {
        // Not used
    }

    public static Endpoint convert(EndpointEntity entity) {
        final Endpoint endpoint = new Endpoint();
        // Add profile
        final Meta meta = new Meta();
        meta.addProfile(EndpointProfile.PROFILE_URI);
        endpoint.setMeta(meta);

        endpoint.setId(new IdType("Endpoint", entity.getId().toString()));
        endpoint.setName(entity.getName());
        endpoint.setAddress(endpoint.getAddress());

        endpoint.setManagingOrganization(new Reference(new IdType("Organization", entity.getOrganization().getId().toString())));
        endpoint.setStatus(entity.getStatus());
        endpoint.setConnectionType(ConnectionTypeConverter.convert(entity.getConnectionType()));

        return endpoint;
    }
}
