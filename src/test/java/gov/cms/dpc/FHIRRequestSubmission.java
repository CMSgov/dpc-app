package gov.cms.dpc;

import ca.uhn.fhir.parser.IParser;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.resources.v1.GroupResource;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

/**
 * Verifies the a user can successfully submit a request to the Group endpoint
 */
@ExtendWith(DropwizardExtensionsSupport.class)
public class FHIRRequestSubmission {
    private static final IParser parser = mock(IParser.class);
    private ResourceExtension resource = ResourceExtension.builder().addResource(new GroupResource(parser)).build();

    // This is required to get get Guice to load correctly.
    // https://github.com/dropwizard/dropwizard/issues/1772
    @BeforeAll
    public static void setup() {
        JerseyGuiceUtils.reset();
    }

    @AfterEach
    public void teardownTest() {
        reset(parser);
    }

    @Test
    public void testDataRequest() {

        final WebTarget target = resource.client().target("/Group/1/$export");
        target.request().accept("application/fhir+json");
        final Response response = target.request().get();
        assertAll(() -> assertEquals(HttpStatus.NO_CONTENT_204, response.getStatus(), "Should have 204 status"),
                () -> assertNotEquals("", response.getHeaderString("Content-Location"), "Should have content location"));
//        final IGenericClient client = ctx.newRestfulGenericClient();

//        final Parameters execute = client
//                .operation()
//                .onInstanceVersion(new IdDt("Group", "1"))
//                .named("$export")
//                .withNoParameters(Parameters.class)
//                .returnResourceType()
////                .encodedJson()
//                .useHttpGet()
//                .execute();
//
//        final Bundle responseBundle = (Bundle) execute.getParameter().get(0).getResource();
//
//        assertEquals(1, responseBundle.getTotal(), "Should only have 1 resource");
    }
}
