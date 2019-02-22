package gov.cms.dpc.web.resources;

import org.hl7.fhir.r4.model.CapabilityStatement;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces("application/fhir+json")
public abstract class AbstractBaseResource {

    public AbstractBaseResource() {
//        Not used
    }

    /**
     * Returns the current API version
     *
     * @return - {@link String} version number
     */
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public abstract String version();

    /**
     * Returns the FHIR capabilities statement
     *
     * @return {@link String} capabilities statement
     */
    @Path("/metadata")
    @GET
    public abstract CapabilityStatement metadata();

    @Path("/Group")
    public abstract AbstractGroupResource groupOperations();

    @Path("/Jobs")
    public abstract AbstractJobResource jobOperations();

    @Path("/Data")
    public abstract AbstractDataResource dataOperations();
}
