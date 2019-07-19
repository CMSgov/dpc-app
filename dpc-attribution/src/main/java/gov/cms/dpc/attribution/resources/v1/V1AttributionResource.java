package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.resources.*;

import javax.inject.Inject;
import javax.ws.rs.Path;

@Path("/v1")
public class V1AttributionResource extends AbstractAttributionResource {

    private final GroupResource gr;
    private final OrganizationResource or;
    private final EndpointResource er;
    private final PractitionerResource pr;
    private final PractitionerRoleResource prr;

    @Inject
    public V1AttributionResource(GroupResource gr, OrganizationResource or, EndpointResource er, PractitionerResource pr, PractitionerRoleResource prr) {
        this.gr = gr;
        this.or = or;
        this.er = er;
        this.pr = pr;
        this.prr = prr;
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

    @Override
    public AbstractPractionerResource providerOperations() {
        return this.pr;
    }

    @Override
    public AbstractPractitionerRoleResource providePractitionerRoleOperations() {
        return this.prr;
    }
}
