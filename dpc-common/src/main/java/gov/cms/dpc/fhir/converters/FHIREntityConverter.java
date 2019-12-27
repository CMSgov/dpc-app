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

    public static FHIREntityConverter initialize() {
        final List<FHIRConverter<?, ?>> converters = ServiceLoaderHelpers.getLoaderStream(FHIRConverter.class)
                .map(l -> (FHIRConverter<?, ?>) l)
                .collect(Collectors.toList());

        return FHIREntityConverter.initialize(converters);

    }

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
