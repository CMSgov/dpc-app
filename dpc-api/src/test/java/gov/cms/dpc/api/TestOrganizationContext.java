package gov.cms.dpc.api;

import java.security.PrivateKey;

/**
 * Helper class to hold all items related to an organization. Helpful when using multiple organization during testing.
 */
public class TestOrganizationContext {

    private String clientToken;
    private String npi;
    private String orgId;
    private String publicKeyId;
    private PrivateKey privateKey;

    public TestOrganizationContext(String clientToken, String npi, String orgId, String publicKeyId, PrivateKey privateKey) {
        this.clientToken = clientToken;
        this.npi = npi;
        this.orgId = orgId;
        this.publicKeyId = publicKeyId;
        this.privateKey = privateKey;
    }

    public String getNpi() {
        return npi;
    }

    public void setNpi(String npi) {
        this.npi = npi;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public String getClientToken() {
        return clientToken;
    }

    public void setClientToken(String clientToken) {
        this.clientToken = clientToken;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }
}
