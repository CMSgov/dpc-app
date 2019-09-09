package gov.cms.dpc.common.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.UUID;

@Entity(name = "organization_tokens")
public class TokenEntity implements Serializable {
    public static final long serialVersionUID = 42L;

    public enum TokenType {
        MACAROON,
        OAUTH
    }

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @ManyToOne
    @JsonIgnore
    private OrganizationEntity organization;

    @Column(name = "type")
    private TokenType tokenType;


    public TokenEntity() {
        // Hibernate required
    }

    public TokenEntity(String tokenID, OrganizationEntity organization, TokenType type) {
        this.id = tokenID;
        this.organization = organization;
        this.tokenType = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }
}
