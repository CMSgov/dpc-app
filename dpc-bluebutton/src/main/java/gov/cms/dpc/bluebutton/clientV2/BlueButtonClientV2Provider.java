package gov.cms.dpc.bluebutton.clientV2;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import gov.cms.dpc.bluebutton.config.BBClientConfiguration;

public class BlueButtonClientV2Provider implements Provider<BlueButtonClientV2> {

    private @Named("fhirContextR4") FhirContext fhirContext;
    private IGenericClient client;
    private BBClientConfiguration config;
    private MetricRegistry metricRegistry;

    public BlueButtonClientV2Provider(BBClientConfiguration config) {
        this.config = config;
    }

    @Inject
    public void setFhirContext(@Named("fhirContextR4") FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    @Inject
    public void setClient(@Named("bbclientR4") IGenericClient client) {
        this.client = client;
    }

    @Inject
    public void setMetricRegistry(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public BlueButtonClientV2 get() {
        return config.isUseBfdMock() ? new MockBlueButtonClientV2(fhirContext) : new BlueButtonClientV2Impl(client, config, metricRegistry);

    }
}
