package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.common.utils.NPIUtil;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * This class is used in environments that need to disable look back
 */
public class EveryoneGetsDataLookBackServiceImpl implements LookBackService {

    @Override
    public String getPractitionerNPIFromRoster(UUID orgID, String providerOrRosterID, String patientMBI) {
        return UUID.randomUUID().toString();
    }

    @Override
    public LookBackAnswer getLookBackAnswer(ExplanationOfBenefit explanationOfBenefit, UUID organizationID, String practitionerNPI, long withinMonth) {
        String npi = NPIUtil.generateNPI();
        return new LookBackAnswer(npi, npi, 1, new Date())
                .addEobBillingPeriod(new Date())
                .addEobOrganization(npi)
                .addEobProviders(List.of(npi));
    }
}
