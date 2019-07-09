package gov.cms.dpc.api.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import org.hl7.fhir.dstu3.model.Patient;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@FHIR
public class TestResource {

    @Inject
    public TestResource() {
        // Not used
    }

    @GET
    public Response base() {
        return Response.status(Response.Status.OK).entity("Hello there!").build();
    }

    @POST
    public Response testValidations(@Valid @Profiled(profile = "https://dpc.cms.gov/fhir/StructureDefinitions/dpc-patient") Patient patient) {
        return Response.ok().build();
    }
}
