package gov.cms.dpc.attribution;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AttributionIntegrationTest {
    private static final DropwizardTestSupport<DPCAttributionConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAttributionService.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "3727"));

    @BeforeAll
    public static void setup() throws Exception {
        APPLICATION.before();
        APPLICATION.getApplication().run("db", "migrate");
        APPLICATION.getApplication().run("seed");
    }

    @BeforeEach()
    public void migrateAndSeed() throws Exception {


    }

    @Test
    public void test() throws IOException {

        HttpGet httpGet = new HttpGet("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/0c527d2e-2e8a-4808-b11d-0fa06baf8254");
        httpGet.setHeader("Accept", MediaType.APPLICATION_JSON);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                List<String> beneficiaries = UnmarshallResponse(response.getEntity());
//                final List<String> beneficiaries = UnmarshallResponse<>(response.getEntity());
                assertEquals(50, beneficiaries.size(), "Should have 50 beneficiaries");
            }
        }

        // Remove some benes



        // Add them back

    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> UnmarshallResponse(HttpEntity entity) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        return (List<T>) mapper.readValue(entity.getContent(), List.class);
    }
}
