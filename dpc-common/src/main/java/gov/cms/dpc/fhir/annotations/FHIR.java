package gov.cms.dpc.fhir.annotations;

import gov.cms.dpc.fhir.FHIRMediaTypes;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import java.lang.annotation.*;


/**
 * Marks the endpoint as complying with the FHIR specification.
 * Automatically adds the {@link Consumes}, and {@link Produces} annotations, in order to give Swagger some hints as to what's going on.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Consumes({FHIRMediaTypes.FHIR_JSON})
@Produces({FHIRMediaTypes.FHIR_JSON})
@Inherited
public @interface FHIR {
}
