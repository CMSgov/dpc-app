package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import io.dropwizard.auth.Auth;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

public abstract class AbstractPatientResource {

    protected AbstractPatientResource() {
        // Not used
    }

    @GET
    public abstract Bundle patientSearch(OrganizationPrincipal organization, String patientMBI);

    @POST
    public abstract Response submitPatient(OrganizationPrincipal organization, Patient patient);
    @POST
    @Path("/$submit")
    public abstract Bundle bulkSubmitPatients(@Auth OrganizationPrincipal organization, Parameters params);

    @GET
    @Path("/{patientID}")
    public abstract Patient getPatient(UUID patientID);

    @DELETE
    @Path("/{patientID}")
    public abstract Response deletePatient(UUID patientID);

    @PUT
    @Path("/{patientID}")
    public abstract Patient updatePatient(UUID patientID, Patient patient);
}
