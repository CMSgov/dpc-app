package gov.cms.dpc.aggregation.service;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

public interface LookBackService {

    /**
     * Checks to see if the explanation of benefits that is associated with the orgID and providerID has a claim
     * within the last withinMonths
     *
     * @param explanationOfBenefit The EoB
     * @param organizationNPI      The organizationNPI
     * @param practitionerNPI      The providerNPI
     * @param withinMonth          The limit of months to qualify for having a claim
     * @return true or false if the EoB matches the organizationID and providerID and has a claim within certain months
     */
    LookBackAnswer getLookBackAnswer(ExplanationOfBenefit explanationOfBenefit, String organizationNPI, String practitionerNPI, long withinMonth);
}
