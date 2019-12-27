package gov.cms.dpc.fhir.converters;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import gov.cms.dpc.fhir.converters.exceptions.DataTranslationException;
import gov.cms.dpc.fhir.converters.exceptions.FHIRConverterException;
import gov.cms.dpc.fhir.converters.exceptions.MissingConverterException;
import gov.cms.dpc.fhir.helpers.ServiceLoaderHelpers;
import org.hl7.fhir.dstu3.model.Base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Conversion engine which handles converting between Java {@link Object} and their corresponding FHIR {@link org.hl7.fhir.dstu3.model.Resource} types.
 * Converters, which implement the {@link FHIRConverter} interface are loaded via the corresponding {@link ServiceLoader}.
 */
public class FHIREntityConverter {

    private static final Logger logger = LoggerFactory.getLogger(FHIREntityConverter.class);

    private final Multimap<Class<? extends Base>, FHIRConverter<?, ?>> fhirResourceMap;
    private final Multimap<Class<?>, FHIRConverter<?, ?>> javaClassMap;
    private final Set<Integer> converterHash;

    FHIREntityConverter() {
        this.fhirResourceMap = ArrayListMultimap.create();
        this.javaClassMap = ArrayListMultimap.create();
        this.converterHash = new HashSet<>();
    }

    /**
     * Attempt to register the given {@link FHIRConverter} with the entity converter
     *
     * @param converter - {@link FHIRConverter} to register
     * @throws FHIRConverterException if a converter is already registered for the given FHIR/Java class pair
     */
    public synchronized void addConverter(FHIRConverter<?, ?> converter) {
        logger.debug("Attempting to add converter: {}", converter);
        // See if we already have something like this
        final int hash = Objects.hash(converter.getFHIRResource(), converter.getJavaClass());
        if (this.converterHash.contains(hash)) {
            throw new FHIRConverterException(String.format("Existing converter for %s and %s", converter.getFHIRResource().getName(), converter.getJavaClass().getName()));
        }

        this.fhirResourceMap.put(converter.getFHIRResource(), converter);
        this.javaClassMap.put(converter.getJavaClass(), converter);

        this.converterHash.add(hash);
    }

    /**
     * Convert the given {@link Base} resource into a corresponding Java class
     *
     * @param targetClass    - {@link Class} of {@link T} to convert FHIR resource into
     * @param sourceResource -{@link S} FHIR resource to convert
     * @param <T>            - {@link T} resulting Java class
     * @param <S>            - {@link S} generic type of FHIR resource
     * @return - {@link T} converted Java object
     * @throws MissingConverterException if no {@link FHIRConverter} is registered between the two classes
     * @throws FHIRConverterException    if the conversion process fails
     */
    @SuppressWarnings("unchecked")
    public <T, S extends Base> T fromFHIR(Class<T> targetClass, S sourceResource) {
        logger.debug("Finding converter from {} to {}", sourceResource, targetClass);
        final FHIRConverter<S, T> converter;
        synchronized (this) {
            converter = this.fhirResourceMap.get(sourceResource.getClass())
                    .stream()
                    .filter(c -> c.getJavaClass().isAssignableFrom(targetClass))
                    .map(c -> (FHIRConverter<S, T>) c)
                    .findAny()
                    .orElseThrow(() -> new MissingConverterException(sourceResource.getClass(), targetClass));
        }

        return handleConversion(() -> converter.fromFHIR(this, sourceResource));
    }

    /**
     * Convert the given Java object into a corresponding FHIR {@link Base} resource
     *
     * @param fhirClass  - {@link T} target FHIR Resource to convert to
     * @param javaSource -{@link S} source Java object to convert
     * @param <T>        - {@link T} resulting FHIR Resource
     * @param <S>        - {@link S} generic type of Java source object
     * @return - {@link T} converted FHIR Resource
     * @throws MissingConverterException if no {@link FHIRConverter} is registered between the two classes
     * @throws FHIRConverterException    if the conversion process fails
     */
    @SuppressWarnings("unchecked")
    public <T extends Base, S> T toFHIR(Class<T> fhirClass, S javaSource) {
        logger.debug("Finding converter from {} to {}", javaSource, fhirClass);
        final FHIRConverter<T, S> converter;
        synchronized (this) {
            converter = this.javaClassMap.get(javaSource.getClass())
                    .stream()
                    .filter(c -> c.getFHIRResource().isAssignableFrom(fhirClass))
                    .map(c -> (FHIRConverter<T, S>) c)
                    .findAny()
                    .orElseThrow(() -> new MissingConverterException(javaSource.getClass(), fhirClass));
        }

        return handleConversion(() -> converter.toFHIR(this, javaSource));
    }

    /**
     * Create a new {@link FHIREntityConverter} using the default converters loaded by the {@link FHIRConverter} service loader
     *
     * @return - {@link FHIREntityConverter} with default set of converters
     */
    public static FHIREntityConverter initialize() {
        final List<FHIRConverter<?, ?>> converters = ServiceLoaderHelpers.getLoaderStream(FHIRConverter.class)
                .map(l -> (FHIRConverter<?, ?>) l)
                .collect(Collectors.toList());

        return FHIREntityConverter.initialize(converters);

    }

    /**
     * Create a new {@link FHIRConverter} with the given {@link Collection} of {@link FHIRConverter}s
     *
     * @param converters - {@link Collection} of {@link FHIRConverter} to register with converter
     * @return - {@link FHIREntityConverter} with only the given {@link FHIRConverter}s registered
     */
    static FHIREntityConverter initialize(Collection<FHIRConverter<?, ?>> converters) {
        final FHIREntityConverter converter = new FHIREntityConverter();

        converters
                .forEach(converter::addConverter);
        return converter;
    }

    private static <T> T handleConversion(Supplier<T> converter) {
        try {
            return converter.get();
        } catch (DataTranslationException e) {
            logger.error("Cannot convert resources.", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unknown exception thrown during conversion", e);
            throw new FHIRConverterException("Cannot convert resources", e);
        }
    }
}
