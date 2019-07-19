package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.jdbi.PatientDAO;
import gov.cms.dpc.attribution.resources.AbstractPatientResource;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.converters.entities.PatientEntityConverter;
import io.dropwizard.hibernate.UnitOfWork;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class PatientResource extends AbstractPatientResource {

    private static final WebApplicationException NOT_FOUND_EXCEPTION = new WebApplicationException("Cannot find patient with given ID", Response.Status.NOT_FOUND);
    private final PatientDAO dao;

    @Inject
    PatientResource(PatientDAO dao) {
        this.dao = dao;
    }

    @GET
    @FHIR
    @Override
    public Bundle searchPatients(@QueryParam("identifier") String patientID, @QueryParam("tag") String organizationToken) {
        return null;
    }

    @POST
    @FHIR
    @UnitOfWork
    @Override
    public Response createPatient(Patient patient) {
        final PatientEntity entity = this.dao.persistPatient(PatientEntity.fromFHIR(patient));

        return Response.status(Response.Status.CREATED)
                .entity(PatientEntityConverter.convert(entity))
                .build();
    }

    @GET
    @Path("/{patientID}")
    @FHIR
    @UnitOfWork
    @Override
    public Patient getPatient(@PathParam("patientID") UUID patientID) {
        final PatientEntity patientEntity = this.dao.getPatient(patientID)
                .orElseThrow(() ->
                        new WebApplicationException("Cannot find patient with given ID", Response.Status.NOT_FOUND));

        return PatientEntityConverter.convert(patientEntity);
    }

    @DELETE
    @Path("/{patientID}")
    @FHIR
    @UnitOfWork
    @Override
    public Response deletePatient(@PathParam("patientID") UUID patientID) {
        final boolean found = this.dao.deletePatient(patientID);

        if (!found) {
            throw NOT_FOUND_EXCEPTION;
        }

        return Response.ok().build();
    }

    @PUT
    @Path("/{patientID}")
    @FHIR
    @Override
    public Patient updatePatient(@PathParam("patientID") UUID patientID, Patient patient) {
        return null;
    }
}
