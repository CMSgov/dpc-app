package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractPatientResource;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.ApiParam;
import org.hl7.fhir.dstu3.model.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class PatientResource extends AbstractPatientResource {

    private final IGenericClient client;

    @Inject
    PatientResource(IGenericClient client) {
        this.client = client;
    }

    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @Override
    public Bundle getPatients(@ApiParam(hidden = true)
                              @Auth OrganizationPrincipal organization,
                              @ApiParam(value = "Patient MBI")
                              @QueryParam("identifier") String patientMBI) {

        final var request = this.client
                .search()
                .forResource(Patient.class)
                .encodedJson()
                .where(Patient.ORGANIZATION.hasId(organization.getOrganization().getId()))
                .returnBundle(Bundle.class);

        if (patientMBI != null && !patientMBI.equals("")) {
            return request
                    .where(Patient.IDENTIFIER.exactly().identifier(patientMBI))
                    .execute();
        }

        return request.execute();
    }

    @FHIR
    @POST
    @Timed
    @ExceptionMetered
    @Override
    public Patient submitPatient(@ApiParam(hidden = true) @Auth OrganizationPrincipal organization, Patient patient) {

        // Set the Managing Organization on the Patient
        final Reference orgReference = new Reference(new IdType("Organization", organization.getOrganization().getId()));
        patient.setManagingOrganization(orgReference);
        final MethodOutcome outcome = this.client
                .create()
                .resource(patient)
                .encodedJson()
                .execute();

        if (!outcome.getCreated()) {
            throw new WebApplicationException("Unable to create Patient", Response.Status.INTERNAL_SERVER_ERROR);
        }

        return (Patient) outcome.getResource();
    }

    @GET
    @FHIR
    @Path("/{patientID}")
    @PathAuthorizer(type = ResourceType.Patient, pathParam = "patientID")
    @Timed
    @ExceptionMetered
    @Override
    public Patient getPatient(@PathParam("patientID") UUID patientID) {
        return this.client
                .read()
                .resource(Patient.class)
                .withId(patientID.toString())
                .encodedJson()
                .execute();
    }

    @DELETE
    @FHIR
    @Path("/{patientID}")
    @PathAuthorizer(type = ResourceType.Patient, pathParam = "patientID")
    @Timed
    @ExceptionMetered
    @Override
    public Response deletePatient(@PathParam("patientID") UUID patientID) {
        this.client
                .delete()
                .resourceById("Patient", patientID.toString())
                .encodedJson()
                .execute();

        return Response.ok().build();
    }

    @PUT
    @Path("/{patientID}")
    @PathAuthorizer(type = ResourceType.Patient, pathParam = "patientID")
    @FHIR
    @Timed
    @ExceptionMetered
    @Override
    public Patient updatePatient(@PathParam("patientID") UUID patientID, Patient patient) {
        final MethodOutcome outcome = this.client
                .update()
                .resource(patient)
                .withId(new IdType("Patient", patientID.toString()))
                .encodedJson()
                .execute();

        final Patient resource = (Patient) outcome.getResource();
        if (resource == null) {
            throw new WebApplicationException("Unable to update Patient", Response.Status.INTERNAL_SERVER_ERROR);
        }
        return resource;
    }
}
