package gov.cms.dpc.web.resources.v1;

import gov.cms.dpc.web.core.Capabilities;
import gov.cms.dpc.web.resources.AbstractBaseResource;
import gov.cms.dpc.web.resources.AbstractGroupResource;
import gov.cms.dpc.web.resources.AbstractJobResource;
import org.hl7.fhir.r4.model.CapabilityStatement;

import javax.inject.Inject;
import javax.ws.rs.Path;


@Path("/v1")
public class BaseResource extends AbstractBaseResource {

    private final AbstractGroupResource gr;
    private final AbstractJobResource jr;

    @Inject
    public BaseResource(GroupResource gr, JobResource jr) {
        this.gr = gr;
        this.jr = jr;
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
}
