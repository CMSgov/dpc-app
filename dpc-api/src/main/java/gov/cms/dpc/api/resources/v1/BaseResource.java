package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.api.core.Capabilities;
import gov.cms.dpc.api.resources.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hl7.fhir.dstu3.model.CapabilityStatement;

import javax.inject.Inject;
import javax.ws.rs.Path;


@Path("/v1")
@Api(value = "Metadata")
public class BaseResource extends AbstractBaseResource {

    private final AbstractGroupResource gr;
    private final AbstractJobResource jr;
    private final AbstractDataResource dr;
    private final AbstractRosterResource rr;
    private final AbstractOrganizationResource or;
    private final AbstractPatientResource par;
    private final AbstractPractionerResource pr;
    private final AbstractDefinitionResource sdr;

    @Inject
    public BaseResource(GroupResource gr,
                        JobResource jr,
                        DataResource dr,
                        RosterResource rr,
                        OrganizationResource or,
                        PatientResource par,
                        PractitionerResource pr,
                        DefinitionResource sdr) {
        this.gr = gr;
        this.jr = jr;
        this.dr = dr;
        this.rr = rr;
        this.or = or;
        this.par = par;
        this.pr = pr;
        this.sdr = sdr;
    }

    @Override
    @ApiOperation(value = "Return the software version", hidden = true)
    public String version() {
        return "Version 1";
    }

    @Override
    @ApiOperation(value = "Get FHIR Metadata", notes = "Returns the FHIR Capabilities statement for the application", response = CapabilityStatement.class)
    public CapabilityStatement metadata() {
        return Capabilities.buildCapabilities();
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
    public AbstractRosterResource rosterOperations() {
        return this.rr;
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
    public AbstractPractionerResource practitionerOperations() {
        return this.pr;
    }
}
