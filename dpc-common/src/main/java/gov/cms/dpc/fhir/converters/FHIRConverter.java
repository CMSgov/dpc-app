package gov.cms.dpc.fhir.converters;

import org.hl7.fhir.instance.model.api.IBase;

public interface FHIRConverter<R extends IBase, C> {

    C fromFHIR(FHIREntityConverter converter, R resource);

    R toFHIR(FHIREntityConverter converter, C entity);

    Class<R> getFHIRResource();

    Class<C> getJavaClass();
}
