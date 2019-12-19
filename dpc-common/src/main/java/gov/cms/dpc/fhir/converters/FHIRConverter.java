package gov.cms.dpc.fhir.converters;

import org.hl7.fhir.dstu3.model.Base;

public interface FHIRConverter<R extends Base, C> {

    C fromFHIR(FHIREntityConverter converter, R resource);

    R toFHIR(FHIREntityConverter converter, C javaClass);

    Class<R> getFHIRResource();

    Class<C> getJavaClass();
}
