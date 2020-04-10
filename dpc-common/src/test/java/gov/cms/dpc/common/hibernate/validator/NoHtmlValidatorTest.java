package gov.cms.dpc.common.hibernate.validator;

import gov.cms.dpc.common.annotations.NoHtml;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Stream;

public class NoHtmlValidatorTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest
    @MethodSource("stringSource")
    public void noHtmlValidatorTest(String value, boolean isEmpty) {

        TestObject testObject = new TestObject();
        testObject.setA(value);
        Set<ConstraintViolation<TestObject>> violations = validator.validate(testObject);
        Assertions.assertEquals(isEmpty, violations.isEmpty());
    }

    private static Stream<Arguments> stringSource() {
        return Stream.of(
                Arguments.of("<img src=x onerror=prompt(1234)>", false),
                Arguments.of("hello", true),
                Arguments.of("hello@gmail.com", true),
                Arguments.of("<script/>", false),
                Arguments.of(null, true),
                Arguments.of("", true),
                Arguments.of("hello\n\rbob", true)
        );
    }

    static class TestObject {

        @NoHtml
        private String a;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }
    }
}
