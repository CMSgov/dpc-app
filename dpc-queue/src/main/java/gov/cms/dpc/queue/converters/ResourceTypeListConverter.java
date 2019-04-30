package gov.cms.dpc.queue.converters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hl7.fhir.dstu3.model.ResourceType;

import javax.persistence.AttributeConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * {@link AttributeConverter} which stores a {@link List} of {@link ResourceType} elements as a single
 * {@link String} column in the database.
 */
public class ResourceTypeListConverter implements AttributeConverter<List<ResourceType>, String>  {
    private static final String LIST_DELIM = ",";

    private static Logger logger = LoggerFactory.getLogger(ResourceTypeListConverter.class);

    @Override
    public String convertToDatabaseColumn(List<ResourceType> attribute) {
        final var joiner = new StringJoiner(LIST_DELIM);
        for (ResourceType type: attribute) {
            joiner.add(type.toString());
        }
        return joiner.toString();
    }

    @Override
    public List<ResourceType> convertToEntityAttribute(String dbData) {
        
        final var resourceList = new ArrayList<ResourceType>();
        if (dbData.isEmpty()) {
            return resourceList;
        }       
        for (String typeString : dbData.split(LIST_DELIM, -1)) {
            try {
                final var type = ResourceType.valueOf(typeString);
                resourceList.add(type);
            } catch (IllegalArgumentException ex) {
                logger.error("Job with an invalid resource type: {}", typeString);
                throw ex;
            }
        }
        return resourceList;
    }
}


