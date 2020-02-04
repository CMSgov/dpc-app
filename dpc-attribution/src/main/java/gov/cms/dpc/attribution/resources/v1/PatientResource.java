package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.jdbi.PatientDAO;
import gov.cms.dpc.attribution.resources.AbstractPatientResource;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.BundleReturnProperties;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.*;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static gov.cms.dpc.attribution.utils.RESTUtils.bulkResourceHandler;

@Api(value = "Patient")
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
    @ApiOperation(value = "Search for Patients", notes = "Search for Patient records, optionally restricting by associated organization." +
            "<p>Must provide ONE OF organization ID, patient MBI, or Patient Resource ID to search for", response = Bundle.class)
    @ApiResponses(@ApiResponse(code = 400, message = "Must have Organization ID or Patient MBI in order to search"))
    @Override
    public List<Patient> searchPatients(
            @ApiParam(value = "Patient resource ID")
            @QueryParam("_id") UUID resourceID,
            @ApiParam(value = "Patient MBI")
            @QueryParam("identifier") String patientMBI,
            @ApiParam(value = "Organization ID")
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
    @ApiOperation(value = "Fetch Patient", notes = "Fetch specific Patient record, irrespective of managing organization.")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Patient with given ID"))
    @UnitOfWork
    @Override
    public Patient getPatient(@ApiParam(value = "Patient resource ID", required = true) @PathParam("patientID") UUID patientID) {
        final PatientEntity patientEntity = this.dao.getPatient(patientID)
                .orElseThrow(() ->
                        new WebApplicationException("Cannot find patient with given ID", Response.Status.NOT_FOUND));

        return this.converter.toFHIR(Patient.class, patientEntity);
    }

    @POST
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Create Patient", notes = "Create a Patient record associated to the Organization listed in the *ManagingOrganization* field. " +
            "If a patient record already exists, a `200` status is returned, along with the existing record.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created Patient"),
            @ApiResponse(code = 200, message = "Patient already exists")
    })
    @Override
    public Response createPatient(Patient patient) {

        final UUID organizationID = FHIRExtractors.getEntityUUID(patient.getManagingOrganization().getReference());
        final String patientMPI = FHIRExtractors.getPatientMPI(patient);

        final Response.Status status;
        final PatientEntity entity;
        // Check to see if Patient already exists, if so, ignore it.
        final List<PatientEntity> patientEntities = this.dao.patientSearch(null, patientMPI, organizationID);
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
    }

    @POST
    @Path("/$submit")
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Bulk submit Patient resources", notes = "FHIR operation for submitting a Bundle of Patient resources, which will be associated to the given Organization.", response = Bundle.class)
    @BundleReturnProperties(bundleType = Bundle.BundleType.COLLECTION)
    @Override
    public List<Patient> bulkSubmitPatients(Parameters params) {
        return bulkResourceHandler(Patient.class, params, this::createPatient);
    }

    @DELETE
    @Path("/{patientID}")
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Delete Patient", notes = "Remove specific Patient record")
    @ApiResponses(@ApiResponse(code = 404, message = "Unable to find Patient to delete"))
    @Override
    public Response deletePatient(@ApiParam(value = "Patient resource ID", required = true) @PathParam("patientID") UUID patientID) {
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
    @ApiOperation(value = "Update Patient record", notes = "Update specific Patient record." +
            "<p>Currently, this method only allows for updating of the Patient first/last name, and BirthDate.")
    @ApiResponses(@ApiResponse(code = 404, message = "Unable to find Patient to update"))
    @Override
    public Response updatePatient(@ApiParam(value = "Patient resource ID", required = true) @PathParam("patientID") UUID patientID, Patient patient) {
        final PatientEntity patientEntity = this.dao.updatePatient(patientID, this.converter.fromFHIR(PatientEntity.class, patient));

        return Response.ok()
                .entity(this.converter.toFHIR(Patient.class, patientEntity))
                .build();
    }
}
