package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.common.annotations.ServiceBaseURL;
import gov.cms.dpc.api.core.Capabilities;
import gov.cms.dpc.api.resources.AbstractBaseResource;
import gov.cms.dpc.api.resources.AbstractDataResource;
import gov.cms.dpc.api.resources.AbstractGroupResource;
import gov.cms.dpc.api.resources.AbstractJobResource;
import org.hl7.fhir.dstu3.model.CapabilityStatement;

import javax.inject.Inject;
import javax.ws.rs.Path;


@Path("/v1")
public class BaseResource extends AbstractBaseResource {

    private final AbstractGroupResource gr;
    private final AbstractJobResource jr;
    private final AbstractDataResource dr;
    private final String baseURL;

    @Inject
    public BaseResource(GroupResource gr, JobResource jr, DataResource dr, @ServiceBaseURL String baseURL) {
        this.gr = gr;
        this.jr = jr;
        this.dr = dr;
        this.baseURL = baseURL;
    }

    @Override
    public String version() {
        return "Version 1";
    }

    @Override
    public CapabilityStatement metadata() {
        return Capabilities.buildCapabilities(this.baseURL, "/v1");
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
