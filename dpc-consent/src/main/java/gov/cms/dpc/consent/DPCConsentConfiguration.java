package gov.cms.dpc.consent;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.common.hibernate.consent.IDPCConsentDatabase;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.configuration.IDPCFHIRConfiguration;
import io.dropwizard.db.DataSourceFactory;
import org.hibernate.validator.constraints.NotEmpty;
import org.knowm.dropwizard.sundial.SundialConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class DPCConsentConfiguration extends TypesafeConfiguration implements IDPCConsentDatabase, IDPCFHIRConfiguration {

    @Valid
    @NotNull
    @JsonProperty("consentdb")
    private DataSourceFactory consentDatabase = new DataSourceFactory();

    @JsonProperty("sundial")
    private SundialConfiguration sundial = new SundialConfiguration();

    @NotEmpty
    private String suppressionFileDir;

    @Valid
    @NotNull
    @JsonProperty("fhir")
    private DPCFHIRConfiguration fhirConfig;

    @NotEmpty
    private String fhirReferenceURL;

    @NotEmpty
    private String consentOrganizationURL;

    @Override
    public DataSourceFactory getConsentDatabase() {
        return consentDatabase;
    }

    public SundialConfiguration getSundial() {
        return sundial;
    }

    public String getSuppressionFileDir() { return suppressionFileDir; }

    public void setSuppressionFileDir(String suppressionFileDir) { this.suppressionFileDir = suppressionFileDir; }

    @Override
    public DPCFHIRConfiguration getFHIRConfiguration() {
        return fhirConfig;
    }

    @Override
    public void setFHIRConfiguration(DPCFHIRConfiguration fhirConfig) {
        this.fhirConfig = fhirConfig;
    }

    public String getFhirReferenceURL() {
        return fhirReferenceURL;
    }

    public void setFhirReferenceURL(String fhirReferenceURL) {
        this.fhirReferenceURL = fhirReferenceURL;
    }

    public String getConsentOrganizationURL() {
        return consentOrganizationURL;
    }

    public void setConsentOrganizationURL(String consentOrganizationURL) {
        this.consentOrganizationURL = consentOrganizationURL;
    }
}
