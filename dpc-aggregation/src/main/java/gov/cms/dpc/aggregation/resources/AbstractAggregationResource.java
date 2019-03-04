package gov.cms.dpc.aggregation.resources;

import javax.ws.rs.Path;

public abstract class AbstractAggregationResource {

    protected AbstractAggregationResource() {

    }

    @Path("/Beneficiary")
    public abstract AbstractBeneficiaryResource beneficiaryOperations();
}
