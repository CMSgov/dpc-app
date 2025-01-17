package gov.cms.dpc.attribution.utils;

import gov.cms.dpc.common.hibernate.attribution.DPCAbstractDAO;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
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
     * @param clazz          - {@link Class} of type of filter {@link Bundle} entries
     * @param params         - {@link Parameters} which has a {@link Parameters#getParameterFirstRep()}
     * @param resourceAction - {@link Function} which performs the actual bulk action for a single {@link BaseResource} of type {@link T}
     * @param <T>            - {@link T} generic type parameter which extends {@link BaseResource}
     * @return - {@link Bundle} containing the processed results from the bulk submission
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseResource> List<T> bulkResourceHandler(Class<T> clazz, Parameters params, Function<T, Response> resourceAction) {
        final Bundle resourceBundle = (Bundle) params.getParameterFirstRep().getResource();
        final Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        // Grab all of the providers and submit them individually (for now)
        // TODO: Optimize insert as part of DPC-490

        return resourceBundle
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getClass().equals(clazz))
                .map(clazz::cast)
                .map(resource -> {
                    final Response response = resourceAction.apply(resource);
                    if (HttpStatus.isSuccess(response.getStatus())) {
                        return (Resource) response.getEntity();
                    }
                    // If there's an error, rethrow the original method
                    throw new WebApplicationException(response);
                })
                .map(r -> (T) r)
                .collect(Collectors.toList());
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
