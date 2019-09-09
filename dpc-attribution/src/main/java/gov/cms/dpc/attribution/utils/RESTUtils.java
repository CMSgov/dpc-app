package gov.cms.dpc.attribution.utils;

import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public static <T extends BaseResource> Bundle bulkResourceHandler(Class<T> clazz, Parameters params, Function<T, Response> resourceAction) {
        final Bundle resourceBundle = (Bundle) params.getParameterFirstRep().getResource();
        final Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        // Grab all of the providers and submit them individually (for now)
        // TODO: Optimize insert as part of DPC-490

        final List<Bundle.BundleEntryComponent> bundleEntries = resourceBundle
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
                .map(resource -> new Bundle.BundleEntryComponent().setResource(resource))
                .collect(Collectors.toList());

        bundle.setEntry(bundleEntries);
        bundle.setTotal(bundleEntries.size());
        return bundle;
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
        final Function<String, UUID> builder = (tag) -> {
            final IdType idType = new IdType(tag);
            return UUID.fromString(idType.getIdPart());
        };
        return parseTokenTag(builder, tokenTag);
    }
}
