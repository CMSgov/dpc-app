package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;

import java.util.UUID;


@Path("/Patient")
@FHIR
public abstract class AbstractPatientResource extends AbstractResourceWithSince {

    protected AbstractPatientResource() {
        // Not used
    }

    @GET
    public abstract Bundle patientSearch(OrganizationPrincipal organization, @NoHtml String patientMBI);

    @POST
    public abstract Response submitPatient(OrganizationPrincipal organization, @Valid @Profiled Patient patient);

    @POST
    @Path("/$submit")
    public abstract Bundle bulkSubmitPatients(OrganizationPrincipal organization, Parameters params);

    @GET
    @Path("/{patientID}")
    public abstract Patient getPatient(UUID patientID);

    @GET
    @Path("/{patientID}/$everything")
    public abstract Resource everything(OrganizationPrincipal organization,
                                        @Valid @Profiled Provenance attestation,
                                        UUID patientId,
                                        @QueryParam("_since") @NoHtml String since,
                                        HttpServletRequest request);

    @DELETE
    @Path("/{patientID}")
    public abstract Response deletePatient(UUID patientID);

    @PUT
    @Path("/{patientID}")
    public abstract Patient updatePatient(UUID patientID, @Valid @Profiled Patient patient);

    @POST
    @Path("/$validate")
    public abstract IBaseOperationOutcome validatePatient(OrganizationPrincipal organization, Parameters parameters);
}
