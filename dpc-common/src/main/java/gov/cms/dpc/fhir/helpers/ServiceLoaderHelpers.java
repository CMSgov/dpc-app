package gov.cms.dpc.fhir.helpers;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ServiceLoaderHelpers {

    private ServiceLoaderHelpers() {
        // Not used
    }

    /**
     * Returns a {@link Stream} of {@link P} from the given service loader implementation
     *
     * @param clazz - {@link Class} of {@link P} to load from {@link ServiceLoader}
     * @param <P>   - {@link P} type parameter from Service Loader
     * @return - {@link Stream} of {@link P}s available from the service loader.
     */
    public static <P> Stream<P> getLoaderStream(Class<P> clazz) {
        Iterable<P> targetStream = () -> createLoader(clazz);
        return StreamSupport.stream(targetStream.spliterator(), false);
    }

    private static <P> Iterator<P> createLoader(Class<P> clazz) {
        final ServiceLoader<P> loader = ServiceLoader.load(clazz);
        return loader.iterator();
    }
}
