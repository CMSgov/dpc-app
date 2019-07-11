package gov.cms.dpc.fhir.configuration;

public interface IDPCFHIRConfiguration {

    DPCFHIRConfiguration getFHIRConfiguration();

    void setFHIRConfiguration(DPCFHIRConfiguration config);
}
