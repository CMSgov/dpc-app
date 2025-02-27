package gov.cms.dpc.fhir.annotations;

import gov.cms.dpc.fhir.FHIRMediaTypes;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import java.lang.annotation.*;

/**
 * Marks the endpoint as complying with the FHIR async API specification
 * Automatically adds the {@link Consumes}, and {@link Produces} annotations to restrict the possible inputs/outputs to the API
 */
@Retention(RetentionPolicy.RUNTIME)
@Consumes({FHIRMediaTypes.FHIR_JSON})
@Target({ElementType.TYPE, ElementType.METHOD})
@Produces({FHIRMediaTypes.FHIR_JSON})
@Inherited
public @interface FHIRAsync {
}
