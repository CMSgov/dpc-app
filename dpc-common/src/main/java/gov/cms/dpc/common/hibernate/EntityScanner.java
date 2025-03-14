package gov.cms.dpc.common.hibernate;

import com.google.common.collect.ImmutableList;
import jakarta.persistence.Entity;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Helper class for scanning a given class path and returning an {@link ImmutableList} of {@link Entity} annotated classes.
 */
public class EntityScanner {

    private static final Logger logger = LoggerFactory.getLogger(EntityScanner.class);

    private EntityScanner() {
        // Not used
    }


    /**
     * Add additional paths to a single prefix, and then call {@link EntityScanner#applicationEntities(List)}
     *
     * @param prefix          - {@link String} default prefix
     * @param additionalPaths - {@link List} of {@link String} additional package prefixes to scan
     * @return - {@link ImmutableList} of {@link Class}
     */
    public static ImmutableList<Class<?>> applicationEntities(String prefix, List<String> additionalPaths) {
        final ArrayList<String> paths = new ArrayList<>(additionalPaths);
        paths.add(prefix);
        return applicationEntities(paths);
    }

    /**
     * Scan the given {@link List} of package prefixes for any {@link Entity} annotated classes
     *
     * @param paths - {@link String} class package to class
     * @return - {@link ImmutableList} of annotated {@link Class}es
     */
    public static ImmutableList<Class<?>> applicationEntities(List<String> paths) {

        final List<Class<?>> collect = paths
                .stream()
                .map(path -> {
                    logger.info("Scanning {} for Hibernate entities", path);
                    final Reflections reflections = new Reflections(path);
                    final Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);
                    logger.info("Found {} Hibernate entities", entities.getClass());
                    if (logger.isDebugEnabled()) {
                        entities.forEach(entity -> logger.debug("Registered {}.", entity.getName()));
                    }
                    return entities;
                })
                .flatMap(Collection::stream)
                .toList();

        return ImmutableList.copyOf(collect);
    }
}
