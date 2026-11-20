package gov.cms.dpc.api.models;

import gov.cms.dpc.common.annotations.NoHtml;
import jakarta.validation.constraints.NotEmpty;

public class KeySignature {
    @NoHtml
    @NotEmpty
    private String key;
    @NoHtml
    @NotEmpty
    private String signature;

    public KeySignature() {}

    public KeySignature(String key, String signature) {
        this.key = key;
        this.signature = signature;
    }

    public String getKey() {
        return key;
    }

    public String getSignature() {
        return signature;
    }
}
