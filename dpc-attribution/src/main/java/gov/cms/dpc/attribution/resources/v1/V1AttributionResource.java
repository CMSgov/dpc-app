package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.resources.AbstractAttributionResource;
import gov.cms.dpc.attribution.resources.AbstractGroupResource;

import javax.inject.Inject;
import javax.ws.rs.Path;

@Path("/v1")
public class V1AttributionResource extends AbstractAttributionResource {

    private final GroupResource gr;

    @Inject
    public V1AttributionResource(GroupResource gr) {
        this.gr = gr;
    }

    @Override
    public AbstractGroupResource groupOperations() {
        return gr;
    }
}
