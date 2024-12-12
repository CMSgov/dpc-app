package gov.cms.dpc.common.annotations;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

public class BindingAnnotationTests {

    // This is the simple class that uses @APIV1 for dependency injection.
    private static class SimpleTestResource {
        private final String publicURL;

        @Inject
        public SimpleTestResource(@APIV1 String publicURL) {
            this.publicURL = publicURL;
        }

        public String getPublicURL() {
            return publicURL;
        }
    }

    @Test
    @DisplayName("Test APIv1 Binding Annotation 🥳")
    public void testAPIV1Injection() {
        // Create the Guice injector and bind @APIV1 annotation to a value
        Injector injector = Guice.createInjector(binder -> {
            binder.bind(String.class)
                .annotatedWith(APIV1.class)
                .toInstance("http://localhost:3002/v1/");  // Bind the value for @APIV1
        });

        // Get the instance of SimpleTestResource from Guice
        SimpleTestResource resource = injector.getInstance(SimpleTestResource.class);

        // Assert that the @APIV1 value was injected correctly
        assertEquals("http://localhost:3002/v1/", resource.getPublicURL(), "The publicURL should be injected correctly");
    }
}
