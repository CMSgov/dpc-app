package gov.cms.dpc.web.resources.v1;

import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.web.core.Capabilities;
import gov.cms.dpc.web.resources.AbstractBaseResource;
import gov.cms.dpc.web.resources.AbstractGroupResource;

import javax.inject.Inject;
import javax.ws.rs.Path;


@Path("/v1")
public class BaseResource extends AbstractBaseResource {

    private final IParser parser;
    private final AbstractGroupResource gr;

    @Inject
    public BaseResource(IParser jsonParser, GroupResource gr) {
        this.parser = jsonParser;
        this.gr = gr;
    }

    @Override
    public String version() {
        return "Version 1";
    }

    @Override
    public String metadata() {
        return parser.encodeResourceToString(Capabilities.buildCapabilities("http://localhost:3002", "/v1"));
    }

    @Override
    public AbstractGroupResource groupOperations() {
        return this.gr;
    }
}
