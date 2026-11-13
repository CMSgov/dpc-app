package gov.cms.dpc.attribution.client.service;

import java.time.Instant;

public class ConsentResult {
    private String consentId;
    private Instant consentDate;
    private boolean isActive;
    private PolicyType policyType;

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public Instant getConsentDate() {
        return consentDate;
    }

    public void setConsentDate(Instant consentDate) {
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


        private final String policyUrl;

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

        public static PolicyType fromPolicyCode(String code) {
            if ("OPTIN".equalsIgnoreCase(code)) {
                return OPT_IN;
            }
            if ("OPTOUT".equalsIgnoreCase(code)) {
                return OPT_OUT;
            }
            return null;
        }

        public String getValue() {return policyUrl;}
    }
}
