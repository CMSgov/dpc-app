package gov.cms.dpc.attribution.client.config;

import gov.cms.dpc.fhir.configuration.FHIRClientConfiguration;

/**
 * Configuration interface for applications loading the attribution client module.
 */
public interface AttributionBundleConfiguration {

    FHIRClientConfiguration getAttributionClientConfiguration();
}
