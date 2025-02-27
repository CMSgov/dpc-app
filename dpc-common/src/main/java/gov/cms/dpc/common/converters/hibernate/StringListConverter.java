package gov.cms.dpc.common.converters.hibernate;

import jakarta.persistence.AttributeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link AttributeConverter} which stores a {@link List} of {@link String} elements as a single {@link String} column in the database.
 */
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String LIST_DELIM = ",";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null) {
            return "";
        }
        return String.join(LIST_DELIM, attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(Arrays.asList(dbData.split(LIST_DELIM)));
    }
}
