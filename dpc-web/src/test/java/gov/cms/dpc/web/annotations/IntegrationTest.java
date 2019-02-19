package gov.cms.dpc.web.annotations;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.*;

/**
 * Marks the test suite (or test method) as being an Integration test, which can only be executed against a fully functioning setup.
 */
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Inherited
public @interface IntegrationTest {
}
