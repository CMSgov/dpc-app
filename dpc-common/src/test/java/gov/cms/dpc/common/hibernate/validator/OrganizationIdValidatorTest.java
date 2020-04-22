package gov.cms.dpc.common.hibernate.validator;

import gov.cms.dpc.common.annotations.OrganizationId;
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

public class OrganizationIdValidatorTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest
    @MethodSource("stringSource")
    public void organizationIdValidatorTest(String value, boolean isValid) {

        TestObject testObject = new TestObject();
        testObject.setA(value);
        Set<ConstraintViolation<TestObject>> violations = validator.validate(testObject);
        Assertions.assertEquals(isValid, violations.isEmpty());
    }

    private static Stream<Arguments> stringSource() {
        return Stream.of(
                Arguments.of("4579310721", true),
                Arguments.of("1234567893", true),
                Arguments.of("1234567894", false),
                Arguments.of("123451", false),
                Arguments.of("12345678921", false),
                Arguments.of(null, false)
        );
    }

    static class TestObject {

        @OrganizationId
        private String a;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }
    }
}

