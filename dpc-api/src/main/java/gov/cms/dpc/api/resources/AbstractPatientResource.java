package gov.cms.dpc.api.resources;

import ca.uhn.fhir.rest.client.api.IGenericClient;
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
public abstract class AbstractPatientResource extends AbstractResourceWithExport {

    protected AbstractPatientResource(IGenericClient client) {
        super(client);
    }

    @GET
    public abstract Bundle patientSearch(OrganizationPrincipal organization, @NoHtml String patientMBI, Integer count, Integer offset);

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
    public abstract Response everything(OrganizationPrincipal organization,
                                        @Valid @Profiled Provenance attestation,
                                        UUID patientId,
                                        @QueryParam("_since") @NoHtml String since,
                                        HttpServletRequest request,
                                        String preferHeader);

    @GET
    @Path("/{patientID}/export")
    public abstract Response export(OrganizationPrincipal organization,
                                    @Valid @Profiled Provenance attestation,
                                    UUID patientId,
                                    @QueryParam("_since") @NoHtml String since,
                                    HttpServletRequest request,
                                    String preferHeader,
                                    @QueryParam("_type") @NoHtml String resourceTypes,
                                    @QueryParam("_outputFormat") @NoHtml String outputFormat);

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
