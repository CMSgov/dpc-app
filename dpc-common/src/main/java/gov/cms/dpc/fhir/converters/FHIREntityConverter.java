package gov.cms.dpc.fhir.converters;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import gov.cms.dpc.fhir.helpers.ServiceLoaderHelpers;
import org.hl7.fhir.dstu3.model.Base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FHIREntityConverter {

    private static final Logger logger = LoggerFactory.getLogger(FHIREntityConverter.class);

    private final Multimap<Class<? extends Base>, FHIRConverter<?, ?>> fhirResourceMap;
    private final Multimap<Class<?>, FHIRConverter<?, ?>> javaClassMap;

    private FHIREntityConverter(Multimap<Class<? extends Base>, FHIRConverter<?, ?>> fhirResourceMap, Multimap<Class<?>, FHIRConverter<?, ?>> javaClassMap) {
        this.fhirResourceMap = fhirResourceMap;
        this.javaClassMap = javaClassMap;
    }

    private FHIREntityConverter() {
        this.fhirResourceMap = ArrayListMultimap.create();
        this.javaClassMap = ArrayListMultimap.create();
    }

    public synchronized void addConverter(FHIRConverter<?, ?> converter) {
        this.fhirResourceMap.put(converter.getFHIRResource(), converter);
        this.javaClassMap.put(converter.getJavaClass(), converter);
    }

    public <T, S extends Base> T fromFHIR(Class<T> targetClass, S sourceResource) {
        final FHIRConverter<S, T> converter;
        synchronized (this) {
            //noinspection unchecked
            converter = this.fhirResourceMap.get(sourceResource.getClass())
                    .stream()
                    .filter(c -> c.getJavaClass().isAssignableFrom(targetClass))
                    .map(c -> (FHIRConverter<S, T>) c)
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("Cannot find converter"));
        }

        return converter.fromFHIR(this, sourceResource);
    }

    public <T extends Base, S> T toFHIR(Class<T> fhirClass, S javaSource) {
        final FHIRConverter<T, S> converter;
        synchronized (this) {
            //noinspection unchecked
            converter = this.javaClassMap.get(javaSource.getClass())
                    .stream()
                    .filter(c -> c.getFHIRResource().isAssignableFrom(fhirClass))
                    .map(c -> (FHIRConverter<T, S>) c)
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException(String.format("Cannot find converter from %s to %s", javaSource.getClass().getName(), fhirClass.getName())));
        }

        return converter.toFHIR(this, javaSource);
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
}
