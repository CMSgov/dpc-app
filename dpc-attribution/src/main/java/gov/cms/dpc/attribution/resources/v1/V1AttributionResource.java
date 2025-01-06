package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.resources.*;

import jakarta.inject.Inject;
import javax.ws.rs.Path;

@Path("/v1")
public class V1AttributionResource extends AbstractAttributionResource {

    private final GroupResource gr;
    private final OrganizationResource or;
    private final EndpointResource er;
    private final PatientResource par;
    private final PractitionerResource pr;

    @Inject
    public V1AttributionResource(GroupResource gr,
                                 OrganizationResource or,
                                 EndpointResource er,
                                 PatientResource par,
                                 PractitionerResource pr) {
        this.gr = gr;
        this.or = or;
        this.er = er;
        this.par = par;
        this.pr = pr;
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
    public AbstractPatientResource patientOperations() {
        return this.par;
    }

    @Override
    public AbstractPractitionerResource providerOperations() {
        return this.pr;
    }
}
