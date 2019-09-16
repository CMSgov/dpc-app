package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.api.auth.annotations.Public;
import gov.cms.dpc.api.core.Capabilities;
import gov.cms.dpc.api.resources.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hl7.fhir.dstu3.model.CapabilityStatement;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;


@Path("/v1")
@Api(value = "Metadata")
public class BaseResource extends AbstractBaseResource {

    private final AbstractCertificateResource cr;
    private final AbstractGroupResource gr;
    private final AbstractJobResource jr;
    private final AbstractDataResource dr;
    private final AbstractEndpointResource er;
    private final AbstractOrganizationResource or;
    private final AbstractPatientResource par;
    private final AbstractPractitionerResource pr;
    private final AbstractDefinitionResource sdr;

    @Inject
    public BaseResource(CertificateResource cr,
                        GroupResource gr,
                        JobResource jr,
                        DataResource dr,
                        EndpointResource er,
                        OrganizationResource or,
                        PatientResource par,
                        PractitionerResource pr,
                        DefinitionResource sdr) {
        this.cr = cr;
        this.gr = gr;
        this.jr = jr;
        this.dr = dr;
        this.er = er;
        this.or = or;
        this.par = par;
        this.pr = pr;
        this.sdr = sdr;
    }

    @Override
    @Public
    @GET
    @Path("/version")
    @ApiOperation(value = "Return the software version", hidden = true)
    public String version() {
        return "Version 1";
    }

    @Override
    @GET
    @Path("/metadata")
    @Public
    @ApiOperation(value = "Get FHIR Metadata", notes = "Returns the FHIR Capabilities statement for the application", response = CapabilityStatement.class)
    public CapabilityStatement metadata() {
        return Capabilities.getCapabilities();
    }

    @Override
    public AbstractCertificateResource certificateOperations() {
        return this.cr;
    }

    @Override
    public AbstractGroupResource groupOperations() {
        return this.gr;
    }

    @Override
    public AbstractJobResource jobOperations() {
        return this.jr;
    }

    @Override
    public AbstractDefinitionResource definitionResourceOperations() {
        return this.sdr;
    }

    @Override
    public AbstractDataResource dataOperations() {
        return this.dr;
    }

    @Override
    public AbstractEndpointResource endpointOperations() {
        return this.er;
    }

    @Override
    public AbstractOrganizationResource organizationOperations() {
        return this.or;
    }

    @Override
    public AbstractPatientResource patientOperations() {
        return this.par;
    }

    @Override
    public AbstractPractitionerResource practitionerOperations() {
        return this.pr;
    }
}
