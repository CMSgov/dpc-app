package gov.cms.dpc.aggregation.service;

import java.util.Date;

public class ConsentResult {
    private String consentId;
    private Date consentDate;
    private boolean isActive;
    private PolicyType policyType;

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public Date getConsentDate() {
        return consentDate;
    }

    public void setConsentDate(Date consentDate) {
        this.consentDate = consentDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public void setPolicyType(PolicyType policyType) {
        this.policyType = policyType;
    }

    public enum PolicyType {
        OPT_IN("http://hl7.org/fhir/ConsentPolicy/opt-in"),
        OPT_OUT("http://hl7.org/fhir/ConsentPolicy/opt-out");


        private String policyUrl;

        PolicyType(String policyUrl) {
            this.policyUrl = policyUrl;
        }

        public static PolicyType fromPolicyUrl(String url) {
            if (url != null) {
                for (PolicyType policyType : PolicyType.values()) {
                    if (policyType.policyUrl.equalsIgnoreCase(url)) {
                        return policyType;
                    }
                }
            }
            return null;
        }

        public String getValue() {return policyUrl;}
    }
}
