package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.resources.AbstractAttributionResource;
import gov.cms.dpc.attribution.resources.AbstractEndpointResource;
import gov.cms.dpc.attribution.resources.AbstractGroupResource;
import gov.cms.dpc.attribution.resources.AbstractOrganizationResource;

import javax.inject.Inject;
import javax.ws.rs.Path;

@Path("/v1")
public class V1AttributionResource extends AbstractAttributionResource {

    private final GroupResource gr;
    private final OrganizationResource or;
    private final EndpointResource er;

    @Inject
    public V1AttributionResource(GroupResource gr, OrganizationResource or, EndpointResource er) {
        this.gr = gr;
        this.or = or;
        this.er = er;
    }

    @Override
    public AbstractGroupResource groupOperations() {
        return gr;
    }

    @Override
    public AbstractOrganizationResource orgOperations() {
        return this.or;
    }

    @Override
    public AbstractEndpointResource endpointOperations() {
        return this.er;
    }
}
