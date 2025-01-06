package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.jdbi.PatientDAO;
import gov.cms.dpc.attribution.resources.AbstractPatientResource;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.BundleReturnProperties;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.fhir.converters.exceptions.FHIRConverterException;
import io.dropwizard.hibernate.UnitOfWork;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;

import jakarta.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static gov.cms.dpc.attribution.utils.RESTUtils.bulkResourceHandler;

public class PatientResource extends AbstractPatientResource {

    private static final WebApplicationException NOT_FOUND_EXCEPTION = new WebApplicationException("Cannot find patient with given ID", Response.Status.NOT_FOUND);
    private final FHIREntityConverter converter;
    private final PatientDAO dao;

    @Inject
    PatientResource(FHIREntityConverter converter, PatientDAO dao) {
        this.dao = dao;
        this.converter = converter;
    }

    @GET
    @FHIR
    @UnitOfWork
    @Override
    public List<Patient> searchPatients(
            @QueryParam("_id") UUID resourceID,
            @QueryParam("identifier") String patientMBI,
            @QueryParam("organization") String organizationReference) {
        if (patientMBI == null && organizationReference == null && resourceID == null) {
            throw new WebApplicationException("Must have one of Patient Identifier, Organization Resource ID, or Patient Resource ID", Response.Status.BAD_REQUEST);
        }

        final String idValue;

        // Extract the Patient MBI from the query param
        if (patientMBI != null) {
            final Identifier patientIdentifier = FHIRExtractors.parseIDFromQueryParam(patientMBI);
            if (!patientIdentifier.getSystem().equals(DPCIdentifierSystem.MBI.getSystem())) {
                throw new WebApplicationException("Must have MBI identifier", Response.Status.BAD_REQUEST);
            }
            idValue = patientIdentifier.getValue();
        } else {
            idValue = null;
        }

        final UUID organizationID = FHIRExtractors.getEntityUUID(organizationReference);
        return this.dao.patientSearch(resourceID, idValue, organizationID)
                .stream()
                .map(p -> this.converter.toFHIR(Patient.class, p))
                .collect(Collectors.toList());
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

        return this.converter.toFHIR(Patient.class, patientEntity);
    }

    @POST
    @FHIR
    @UnitOfWork
    @Override
    public Response createPatient(Patient patient) {
        try {
            final UUID organizationID = FHIRExtractors.getEntityUUID(patient.getManagingOrganization().getReference());
            final String patientMBI = FHIRExtractors.getPatientMBI(patient);

            final Response.Status status;
            final PatientEntity entity;
            // Check to see if Patient already exists, if so, ignore it.
            final List<PatientEntity> patientEntities = this.dao.patientSearch(null, patientMBI, organizationID);
            if (!patientEntities.isEmpty()) {
                status = Response.Status.OK;
                entity = patientEntities.get(0);
            } else {
                status = Response.Status.CREATED;
                entity = this.dao.persistPatient(this.converter.fromFHIR(PatientEntity.class, patient));
            }

            return Response.status(status)
                    .entity(this.converter.toFHIR(Patient.class, entity))
                    .build();
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid Patient resource", HttpStatus.UNPROCESSABLE_ENTITY_422);
        }
    }

    @POST
    @Path("/$submit")
    @FHIR
    @UnitOfWork
    @BundleReturnProperties(bundleType = Bundle.BundleType.COLLECTION)
    @Override
    public List<Patient> bulkSubmitPatients(Parameters params) {
        return bulkResourceHandler(Patient.class, params, this::createPatient);
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
        try {
            final PatientEntity patientEntity = this.dao.updatePatient(patientID, this.converter.fromFHIR(PatientEntity.class, patient));

            return Response.ok()
                    .entity(this.converter.toFHIR(Patient.class, patientEntity))
                    .build();
        } catch (FHIRConverterException e) {
            throw new WebApplicationException("Invalid Patient resource", HttpStatus.UNPROCESSABLE_ENTITY_422);
        }
    }
}
