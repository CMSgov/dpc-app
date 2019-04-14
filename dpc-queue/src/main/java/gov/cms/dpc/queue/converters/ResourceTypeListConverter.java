package gov.cms.dpc.queue.converters;

import gov.cms.dpc.queue.models.JobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * {@link AttributeConverter} which stores a {@link List} of {@link JobModel.ResourceType} elements as a single
 * {@link String} column in the database.
 */
public class ResourceTypeListConverter implements AttributeConverter<List<JobModel.ResourceType>, String>  {
    private static final String LIST_DELIM = ",";

    private static Logger logger = LoggerFactory.getLogger(ResourceTypeListConverter.class);

    @Override
    public String convertToDatabaseColumn(List<JobModel.ResourceType> attribute) {
        StringJoiner joiner = new StringJoiner(LIST_DELIM);
        for (JobModel.ResourceType type: attribute) {
            joiner.add(type.toString());
        }
        return joiner.toString();
    }

    @Override
    public List<JobModel.ResourceType> convertToEntityAttribute(String dbData) {
        var resourceList = new ArrayList<JobModel.ResourceType>();
        for (String typeString: dbData.split(LIST_DELIM)) {
            try {
                final var type = JobModel.ResourceType.valueOf(typeString);
                resourceList.add(type);
            } catch (IllegalArgumentException ex) {
                logger.error("Job with an invalid resource type: {}", typeString);
                throw ex;
            }
        }
        return resourceList;
    }
}


