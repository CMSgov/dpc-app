package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.jdbi.PatientDAO;
import gov.cms.dpc.attribution.resources.AbstractPatientResource;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.converters.entities.PatientEntityConverter;
import io.dropwizard.hibernate.UnitOfWork;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PatientResource extends AbstractPatientResource {

    private static final WebApplicationException NOT_FOUND_EXCEPTION = new WebApplicationException("Cannot find patient with given ID", Response.Status.NOT_FOUND);
    private final PatientDAO dao;

    @Inject
    PatientResource(PatientDAO dao) {
        this.dao = dao;
    }

    @GET
    @FHIR
    @UnitOfWork
    @Override
    public Bundle searchPatients(@QueryParam("identifier") String patientMBI, @QueryParam("organization") String organizationReference) {
        if (patientMBI == null && organizationReference == null) {
            throw new WebApplicationException("Must have either Patient Identifier or Organization Resource ID", Response.Status.BAD_REQUEST);
        }
        final Identifier patientIdentifier = FHIRExtractors.parseIDFromQueryParam(patientMBI);
        if (!patientIdentifier.getSystem().equals(DPCIdentifierSystem.MBI.getSystem())) {
            throw new WebApplicationException("Must have MBI identifier", Response.Status.BAD_REQUEST);
        }

        final UUID organizationID = FHIRExtractors.getEntityUUID(organizationReference);
        final List<Bundle.BundleEntryComponent> patientEntries = this.dao.patientSearch(patientIdentifier.getValue(), organizationID)
                .stream()
                .map(PatientEntityConverter::convert)
                .map(patient -> new Bundle.BundleEntryComponent().setResource(patient))
                .collect(Collectors.toList());

        final Bundle searchBundle = new Bundle();
        searchBundle.setType(Bundle.BundleType.SEARCHSET);
        searchBundle.setTotal(patientEntries.size());
        searchBundle.setEntry(patientEntries);
        return searchBundle;

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
    @UnitOfWork
    @Override
    public Response updatePatient(@PathParam("patientID") UUID patientID, Patient patient) {
        final PatientEntity patientEntity = this.dao.updatePatient(patientID, PatientEntity.fromFHIR(patient));

        return Response.ok()
                .entity(PatientEntityConverter.convert(patientEntity))
                .build();
    }
}
