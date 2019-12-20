package gov.cms.dpc.fhir.converters.rewrite;

import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.fhir.converters.FHIRConverter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.Coding;

public class ConnectionTypeConverter implements FHIRConverter<Coding, EndpointEntity.ConnectionType> {

    public ConnectionTypeConverter() {
        // Not used
    }

    @Override
    public EndpointEntity.ConnectionType fromFHIR(FHIREntityConverter converter, Coding resource) {
        final EndpointEntity.ConnectionType connectionType = new EndpointEntity.ConnectionType();
        connectionType.setCode(resource.getCode());
        connectionType.setSystem(resource.getSystem());

        return connectionType;
    }

    @Override
    public Coding toFHIR(FHIREntityConverter converter, EndpointEntity.ConnectionType javaClass) {
        final Coding coding = new Coding();
        coding.setSystem(javaClass.getSystem());
        coding.setCode(javaClass.getCode());

        return coding;
    }

    @Override
    public Class<Coding> getFHIRResource() {
        return Coding.class;
    }

    @Override
    public Class<EndpointEntity.ConnectionType> getJavaClass() {
        return EndpointEntity.ConnectionType.class;
    }
}
