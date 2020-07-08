package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.validations.profiles.AttestationProfile;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import io.dropwizard.auth.Auth;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;


@Path("/Patient")
@FHIR
public abstract class AbstractPatientResource {

    protected AbstractPatientResource() {
        // Not used
    }

    @GET
    public abstract Bundle patientSearch(OrganizationPrincipal organization, @NoHtml String patientMBI);

    @POST
    public abstract Response submitPatient(OrganizationPrincipal organization, @Valid @Profiled(profile = PatientProfile.PROFILE_URI) Patient patient);
    @POST
    @Path("/$submit")
    public abstract Bundle bulkSubmitPatients(@Auth OrganizationPrincipal organization, Parameters params);

    @GET
    @Path("/{patientID}")
    public abstract Patient getPatient(UUID patientID);

    @GET
    @Path("/{patientID}/$everything")
    public abstract Resource everything(OrganizationPrincipal organization, @Valid @Profiled(profile = AttestationProfile.PROFILE_URI) Provenance attestation, UUID patientId);

    @DELETE
    @Path("/{patientID}")
    public abstract Response deletePatient(UUID patientID);

    @PUT
    @Path("/{patientID}")
    public abstract Patient updatePatient(UUID patientID, @Valid @Profiled(profile = PatientProfile.PROFILE_URI) Patient patient);

    @POST
    @Path("/$validate")
    public abstract IBaseOperationOutcome validatePatient(OrganizationPrincipal organization, Parameters parameters);
}
