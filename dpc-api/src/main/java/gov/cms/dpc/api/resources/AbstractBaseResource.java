package gov.cms.dpc.api.resources;

import org.hl7.fhir.dstu3.model.CapabilityStatement;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces("application/fhir+json")
public abstract class AbstractBaseResource {

    protected AbstractBaseResource() {
//        Not used
    }

    // Auth Operations

    @Path("/Key")
    public abstract AbstractKeyResource keyOperations();

    @Path("/Token")
    public abstract AbstractTokenResource tokenOperations();

    // FHIR Endpoints

    @Path("/Data")
    public abstract AbstractDataResource dataOperations();

    @Path("/Endpoint")
    public abstract AbstractEndpointResource endpointOperations();

    @Path("/Group")
    public abstract AbstractGroupResource groupOperations();

    @Path("/Jobs")
    public abstract AbstractJobResource jobOperations();

    @Path("/Organization")
    public abstract AbstractOrganizationResource organizationOperations();

    @Path("/Admin")
    public abstract AbstractAdminResource adminOperations();

    @Path("/Patient")
    public abstract AbstractPatientResource patientOperations();

    @Path("/Practitioner")
    public abstract AbstractPractitionerResource practitionerOperations();

    @Path("/StructureDefinition")
    public abstract AbstractDefinitionResource definitionResourceOperations();

    @Path("/IpAddress")
    public abstract AbstractIpAddressResource ipAddressOperations();

    /**
     * Returns the FHIR capabilities statement
     *
     * @return {@link String} capabilities statement
     */
    @Path("/metadata")
    @GET
    public abstract CapabilityStatement metadata();

    /**
     * Returns the current API version
     *
     * @return - {@link String} version number
     */
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public abstract String version();
}
