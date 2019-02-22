package gov.cms.dpc.web.resources.v1;

import gov.cms.dpc.web.core.Capabilities;
import gov.cms.dpc.web.resources.AbstractBaseResource;
import gov.cms.dpc.web.resources.AbstractDataResource;
import gov.cms.dpc.web.resources.AbstractGroupResource;
import gov.cms.dpc.web.resources.AbstractJobResource;
import org.hl7.fhir.r4.model.CapabilityStatement;

import javax.inject.Inject;
import javax.ws.rs.Path;


@Path("/v1")
public class BaseResource extends AbstractBaseResource {

    private final AbstractGroupResource gr;
    private final AbstractJobResource jr;
    private final AbstractDataResource dr;

    @Inject
    public BaseResource(GroupResource gr, JobResource jr, DataResource dr) {
        this.gr = gr;
        this.jr = jr;
        this.dr = dr;
    }

    @Override
    public String version() {
        return "Version 1";
    }

    @Override
    public CapabilityStatement metadata() {
        return Capabilities.buildCapabilities("http://localhost:3002", "/v1");
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
    public AbstractDataResource dataOperations() {
        return this.dr;
    }
}
