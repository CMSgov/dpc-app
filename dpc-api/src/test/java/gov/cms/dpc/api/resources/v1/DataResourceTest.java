package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.core.FileManager;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
class DataResourceTest {

    private static final ResourceExtension RESOURCE = buildDataResource();

    private DataResourceTest() {
        // Not used
    }

    @Test
    void streamingTest() {

        final Response response = RESOURCE.target("/Data/test.ndjson")
                .request()
                .get();

        final String entity = response.readEntity(String.class);
        assertEquals("This is a test", entity);
        assertNotNull(response.getHeaderString(HttpHeaders.ETAG), "Should have eTag");
    }

    private static ResourceExtension buildDataResource() {

        final FileManager manager = Mockito.mock(FileManager.class);
        Mockito.when(manager.getFile(Mockito.anyString())).thenAnswer(answer -> {
            final File tempPath = FileUtils.getTempDirectory();
            final File file = File.createTempFile("test", ".ndjson", tempPath);
            FileUtils.write(file, "This is a test", StandardCharsets.UTF_8);
            return file;
        });


        final DataResource dataResource = new DataResource(manager);
        final FhirContext ctx = FhirContext.forDstu3();

        return APITestHelpers.buildResourceExtension(ctx, Collections.singletonList(dataResource), Collections.emptyList(), false);
    }
}
