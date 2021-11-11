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
    public void noHtmlValidatorTest(String value, boolean isValid) {

        TestObject testObject = new TestObject();
        testObject.setA(value);
        Set<ConstraintViolation<TestObject>> violations = validator.validate(testObject);
        Assertions.assertEquals(isValid, violations.isEmpty());
    }

    private static Stream<Arguments> stringSource() {
        return Stream.of(
                Arguments.of("<img src=x onerror=prompt(1234)>", false),
                Arguments.of("hello", true),
                Arguments.of("hello@gmail.com", true),
                Arguments.of("<script/>", false),
                Arguments.of(null, true),
                Arguments.of("", true),
                Arguments.of("hello\n\rbob", true),
                Arguments.of("http://localhost/hi?hello=1234", true),
                Arguments.of("<SCRIPT SRC=http://xss.rocks/xss.js></SCRIPT>", false),
                Arguments.of("javascript:/*--></title></style></textarea></script></xmp><svg/onload='+/\"/+/onmouseover=1/+/[*/[]/+alert(1)//'>", false),
                Arguments.of("<IMG SRC=\"javascript:alert('XSS');\">", false),
                Arguments.of("<IMG SRC=javascript:alert('XSS')>", false),
                Arguments.of("<IMG SRC=JaVaScRiPt:alert('XSS')>", false),
                Arguments.of("<IMG SRC=javascript:alert(&quot;XSS&quot;)>", false),
                Arguments.of("<IMG SRC=`javascript:alert(\"RSnake says, 'XSS'\")`>", false),
                Arguments.of("\\<a onmouseover=\"alert(document.cookie)\"\\>xxs link\\</a\\>", false),
                Arguments.of("\\<a onmouseover=alert(document.cookie)\\>xxs link\\</a\\>", false),
                Arguments.of("<IMG \"\"\"><SCRIPT>alert(\"XSS\")</SCRIPT>\"\\>", false),
                Arguments.of("<IMG SRC=javascript:alert(String.fromCharCode(88,83,83))>", false),
                Arguments.of("perl -e 'print \"<IMG SRC=java\\0script:alert(\\\"XSS\\\")>\";' > out", false),
                Arguments.of("<BODY onload!#$%&()*~+-_.,:;?@[/|\\]^`=alert(\"XSS\")>", false),
                Arguments.of("<<SCRIPT>alert(\"XSS\");//\\<</SCRIPT>", false),
                Arguments.of("<STYLE>li {list-style-image: url(\"javascript:alert('XSS')\");}</STYLE><UL><LI>XSS</br>\n", false),
                Arguments.of("<!--[if gte IE 4]>\n" +
                        "<SCRIPT>alert('XSS');</SCRIPT>\n" +
                        "<![endif]-->", false),
                Arguments.of("&#X000003C;", false),
                Arguments.of("&#0000060;", false),
                Arguments.of("/?param=<data:text/html;base64,PHNjcmlwdD5hbGVydCgnWFNTJyk8L3NjcmlwdD4=", false),
                Arguments.of("Sherlock & Watson", true)
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
