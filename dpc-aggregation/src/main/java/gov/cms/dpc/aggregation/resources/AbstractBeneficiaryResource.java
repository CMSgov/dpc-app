package gov.cms.dpc.aggregation.resources;

import org.hl7.fhir.dstu3.model.Patient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/Beneficiary")
public abstract class AbstractBeneficiaryResource {
    protected AbstractBeneficiaryResource() {

    }

    @GET
    @Path("/{beneficiaryID}")
    public abstract Patient getBeneficiary(String beneficiaryID);
}
