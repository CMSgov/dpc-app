package gov.cms.dpc.aggregation.resources.v1;

import com.google.inject.Inject;
import gov.cms.dpc.aggregation.AggregationEngine;
import gov.cms.dpc.aggregation.resources.AbstractBeneficiaryResource;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;


public class BeneficiaryResource extends AbstractBeneficiaryResource {

    private static final Logger logger = LoggerFactory.getLogger(BeneficiaryResource.class);

    private final AggregationEngine engine;

    @Inject
    public BeneficiaryResource(AggregationEngine engine) {
        this.engine = engine;
    }

    @Path("/beneficiaryID}")
    @GET
    @Override
    public Patient getBeneficiary(@PathParam("beneficiaryID") String beneficiaryID) {
        // TODO: Get actual patient from aggregation engine
        return new Patient();
    }
}
