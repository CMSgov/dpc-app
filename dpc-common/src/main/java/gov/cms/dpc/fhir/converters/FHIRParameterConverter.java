package gov.cms.dpc.fhir.converters;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.instance.model.api.IBaseResource;

import javax.inject.Inject;
import javax.ws.rs.ext.ParamConverter;

public class FHIRParameterConverter <T extends IBaseResource> implements ParamConverter<T> {

    private final FhirContext ctx;

    @Inject
    FHIRParameterConverter(FhirContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public T fromString(String value) {
        return null;
    }

    @Override
    public String toString(T value) {
        return null;
    }
}
