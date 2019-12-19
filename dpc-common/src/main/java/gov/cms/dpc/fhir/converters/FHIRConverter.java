package gov.cms.dpc.fhir.converters;

import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.Resource;

public interface FHIRConverter<R extends Base, C> {

    C fromFHIR(R resource);

    R toFHIR(C javaClass);

    Class<R> getFHIRResource();

    Class<C> getJavaClass();
}
