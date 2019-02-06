package gov.cms.dpc.resources;

import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.core.Capabilities;
import gov.cms.dpc.resources.v1.GroupResource;

import javax.inject.Inject;
import javax.ws.rs.Path;


@Path("/v1")
public class V1BaseResource extends AbstractApplicationRoutes {

    private final IParser parser;
    private final GroupResource gr;

    @Inject
    public V1BaseResource(IParser jsonParser, GroupResource gr) {
        this.parser = jsonParser;
        this.gr = gr;
    }

    @Override
    public String version() {
        return "Version 1";
    }

    @Override
    public String metadata() {
        return parser.encodeResourceToString(Capabilities.buildCapabilities());
    }

    @Override
    public GroupResource groupOperations() {
        return this.gr;
    }
}
