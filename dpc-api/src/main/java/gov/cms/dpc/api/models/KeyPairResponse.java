package gov.cms.dpc.api.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.api.converters.BakeryKeyPairDeserializer;
import gov.cms.dpc.api.converters.BakeryKeyPairSerializer;
import gov.cms.dpc.common.converters.jackson.OffsetDateTimeToStringConverter;
import gov.cms.dpc.common.converters.jackson.StringToOffsetDateTimeConverter;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * Response containing the
 */
public class KeyPairResponse {

    @NotEmpty
    private String algorithm;

    @NotEmpty
    private String environment;

    @NotNull
    @JsonSerialize(using = BakeryKeyPairSerializer.class)
    @JsonDeserialize(using = BakeryKeyPairDeserializer.class)
    private BakeryKeyPair keyPair;

    @NotNull
    @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    private OffsetDateTime createdOn;

    @NotEmpty
    private String createdBy;

    public KeyPairResponse() {
        // Jackson required
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public BakeryKeyPair getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(BakeryKeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public OffsetDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(OffsetDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
