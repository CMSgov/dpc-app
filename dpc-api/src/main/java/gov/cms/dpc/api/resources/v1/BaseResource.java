package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.api.core.Capabilities;
import gov.cms.dpc.api.resources.*;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.common.annotations.Public;
import gov.cms.dpc.common.utils.PropertiesProvider;
import io.swagger.annotations.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.hl7.fhir.dstu3.model.CapabilityStatement;



@Api(value = "Metadata")
@Path("/v1")
public class BaseResource extends AbstractBaseResource {

    private final AbstractKeyResource kr;
    private final AbstractTokenResource tr;
    private final AbstractGroupResource gr;
    private final AbstractJobResource jr;
    private final AbstractDataResource dr;
    private final AbstractOrganizationResource or;
    private final AbstractPatientResource par;
    private final AbstractPractitionerResource pr;
    private final AbstractDefinitionResource sdr;
    private final AbstractAdminResource ar;
    private final PropertiesProvider pp;
    private final AbstractIpAddressResource ip;
    private final String baseURL;
    @Inject
    public BaseResource(KeyResource kr,
                        TokenResource tr,
                        GroupResource gr,
                        JobResource jr,
                        DataResource dr,
                        OrganizationResource or,
                        PatientResource par,
                        PractitionerResource pr,
                        DefinitionResource sdr,
                        AdminResource ar,
                        IpAddressResource ip,
                        @APIV1 String baseURL) {
        this.kr = kr;
        this.tr = tr;
        this.gr = gr;
        this.jr = jr;
        this.dr = dr;
        this.or = or;
        this.par = par;
        this.pr = pr;
        this.sdr = sdr;
        this.ar = ar;
        this.pp = new PropertiesProvider();
        this.ip = ip;
        this.baseURL = baseURL;
    }

    @Override
    @Public
    @GET
    @Path("/version")
    @ApiOperation(value = "Return the application build version")
    @Consumes(value = "*/*")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return this.pp.getBuildVersion();
    }

    @Override
    @GET
    @Path("/metadata")
    @Public
    @ApiOperation(value = "Get FHIR Metadata", notes = "Returns the FHIR Capabilities statement for the application", response = CapabilityStatement.class)
    @ApiResponses(@ApiResponse(code = 200, message = "Successful operation", examples = @Example(@ExampleProperty(value = ""))))
    public CapabilityStatement metadata() {
        return Capabilities.getCapabilities(baseURL);
    }

    @Override
    public AbstractKeyResource keyOperations() {
        return this.kr;
    }

    @Override
    public AbstractTokenResource tokenOperations() {
        return this.tr;
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
    public AbstractOrganizationResource organizationOperations() {
        return this.or;
    }

    @Override
    public AbstractAdminResource adminOperations() {
        return this.ar;
    }

    @Override
    public AbstractPatientResource patientOperations() {
        return this.par;
    }

    @Override
    public AbstractPractitionerResource practitionerOperations() {
        return this.pr;
    }

    @Override
    public AbstractIpAddressResource ipAddressOperations() {
        return this.ip;
    }
}
