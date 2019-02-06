package gov.cms.dpc.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.core.AbstractApplicationRoutes;
import gov.cms.dpc.core.Capabilities;
import org.hl7.fhir.r4.model.CapabilityStatement;

import javax.inject.Inject;
import javax.ws.rs.Path;

@Path("/v1")
public class V1BaseResource extends AbstractApplicationRoutes {

    private final IParser parser;

    @Inject
    public V1BaseResource(IParser jsonParser) {
        this.parser = jsonParser;
    }

    @Override
    public String version() {
        return "Version 1";
    }

    @Override
    public String metadata() {
        return parser.encodeResourceToString(Capabilities.buildCapabilities());
    }
}
