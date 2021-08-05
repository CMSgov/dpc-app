package gov.cms.dpc.bluebutton.clientV2;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.apache.http.client.HttpClient;

public class R4ClientProvider implements Provider<IGenericClient> {

    private FhirContext fhirContext;
    private HttpClient httpClient;
    private String url;

    public R4ClientProvider(String url) {
        this.url = url;
    }

    @Inject
    public void setFhirContext(@Named("fhirContextR4") FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    @Inject
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public IGenericClient get() {
        fhirContext.getRestfulClientFactory().setHttpClient(httpClient);
        return fhirContext.newRestfulGenericClient(url);
    }
}
