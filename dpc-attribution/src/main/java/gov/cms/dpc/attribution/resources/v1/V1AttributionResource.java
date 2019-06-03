package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.resources.AbstractAttributionResource;
import gov.cms.dpc.attribution.resources.AbstractGroupResource;
import gov.cms.dpc.attribution.resources.AbstractOrganizationResource;

import javax.inject.Inject;
import javax.ws.rs.Path;

@Path("/v1")
public class V1AttributionResource extends AbstractAttributionResource {

    private final GroupResource gr;
    private final OrganizationResource or;

    @Inject
    public V1AttributionResource(GroupResource gr, OrganizationResource or) {
        this.gr = gr;
        this.or = or;
    }

    @Override
    public AbstractGroupResource groupOperations() {
        return gr;
    }

    @Override
    public AbstractOrganizationResource orgOperations() {
        return this.or;
    }
}
