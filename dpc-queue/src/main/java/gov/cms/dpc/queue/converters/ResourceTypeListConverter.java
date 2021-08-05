package gov.cms.dpc.queue.converters;

import gov.cms.dpc.fhir.DPCResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * {@link AttributeConverter} which stores a {@link List} of {@link DPCResourceType} elements as a single
 * {@link String} column in the database.
 */
public class ResourceTypeListConverter implements AttributeConverter<List<DPCResourceType>, String>  {
    private static final String LIST_DELIM = ",";

    private static Logger logger = LoggerFactory.getLogger(ResourceTypeListConverter.class);

    @Override
    public String convertToDatabaseColumn(List<DPCResourceType> attribute) {
        final var joiner = new StringJoiner(LIST_DELIM);
        for (DPCResourceType type: attribute) {
            joiner.add(type.toString());
        }
        return joiner.toString();
    }

    @Override
    public List<DPCResourceType> convertToEntityAttribute(String dbData) {
        if (dbData.isEmpty()) {
            return List.of();
        }
        final var resourceList = new ArrayList<DPCResourceType>();
        for (String typeString: dbData.split(LIST_DELIM, -1)) {
            try {
                final var type = DPCResourceType.valueOf(typeString);
                resourceList.add(type);
            } catch (IllegalArgumentException ex) {
                logger.error("Job with an invalid resource type: {}", typeString);
                throw ex;
            }
        }
        return resourceList;
    }
}


