package gov.cms.dpc.aggregation.resources.v1;

import com.google.inject.Inject;
import gov.cms.dpc.aggregation.resources.AbstractAggregationResource;
import gov.cms.dpc.aggregation.resources.AbstractBeneficiaryResource;

import javax.ws.rs.Path;

@Path("/v1")
public class V1AggregationResource extends AbstractAggregationResource {

    private final BeneficiaryResource br;

    @Inject
    public V1AggregationResource(BeneficiaryResource br) {
        this.br = br;
    }

    @Override
    public AbstractBeneficiaryResource beneficiaryOperations() {
        return br;
    }
}
