package gov.cms.dpc.common.converters;

import gov.cms.dpc.common.converters.hibernate.StringListConverter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringListConverterUnitTest {

    static StringListConverter converter = new StringListConverter();

    @Test
    void testConvertToDatabaseColumn() {
        List<String> attributes = List.of("foo", "bar", "baz");
        String expected = String.join(",", attributes);
        assertEquals(expected, converter.convertToDatabaseColumn(attributes));
    }

    @Test
    void testConvertToDatabaseColumn_nullValue() {
        assertEquals("", converter.convertToDatabaseColumn(null));
    }

    @Test
    void testConvertToEntityAttribute() {
        String dbData = "foo,bar,baz";
        List<String> expected = new ArrayList<>(Arrays.asList(dbData.split(",")));
        assertEquals(expected, converter.convertToEntityAttribute(dbData));
    }

    @Test
    void testConvertToEntityAttribute_emptyString() {
        assertEquals(List.of(), converter.convertToEntityAttribute(""));
    }
}
