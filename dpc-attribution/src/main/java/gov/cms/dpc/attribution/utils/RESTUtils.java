package gov.cms.dpc.attribution.utils;

import gov.cms.dpc.common.hibernate.attribution.DPCAbstractDAO;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Resource;

import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RESTUtils {

    private RESTUtils() {
        // Not used
    }

    /**
     * Helper method for bulk submitting a {@link Bundle} of specific resources
     *
     * @param resources Stream of {@link Resource}s to submit
     * @param action A function that performs the insert/update on each {@link Resource}
     * @param dao A DAO used for flushing the Hibernate cache
     * @param batchSize The batch size used for sending inserts/updates to the DB
     * @return A list of the inserted {@link Resource}s
     * @param <R> Any FHIR {@link Resource}
     */
    public static <R extends Resource> List<R> bulkResourceHandler(Stream<R> resources, UnaryOperator<R> action, DPCAbstractDAO<?> dao, int batchSize) {
        AtomicInteger index = new AtomicInteger();

        return resources
            // Insert/Update the resource
            .map(resource -> {
                try {
                    return action.apply(resource);
                } catch(Exception e) {
                    throw new WebApplicationException("Could not insert resource");
                }
            })
            // Flush and clear the Hibernate cache after each batch
            .map(resource -> {
                if (index.incrementAndGet() % batchSize == 0) {
                    dao.cleanUpBatch();
                }
                return resource;
            })
            .collect(Collectors.toList());
    }

    /**
     * Extract specific value from Token tag.
     * This works by splitting the string on the '|' character and passing the right hand value to the builder.
     *
     * @param builder  - {@link Function} for building specific return value.
     * @param tokenTag - {@link String} token value to parse
     * @param <T>      - {@link T} generic return type
     * @return - {@link T}
     */
    public static <T> T parseTokenTag(Function<String, T> builder, String tokenTag) {
        final int idx = tokenTag.indexOf('|');
        if (idx < 0) {
            throw new WebApplicationException("Malformed tokenTag", HttpStatus.UNPROCESSABLE_ENTITY_422);
        }

        return builder.apply(tokenTag.substring(idx + 1));
    }

    public static UUID tokenTagToUUID(String tokenTag) {
        final Function<String, UUID> builder = tag -> {
            final IdType idType = new IdType(tag);
            return UUID.fromString(idType.getIdPart());
        };
        return parseTokenTag(builder, tokenTag);
    }
}
