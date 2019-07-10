package gov.cms.dpc.attribution.cli;

import gov.cms.dpc.fhir.converters.FHIRResourceConverter;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;

import java.util.List;
import java.util.stream.Collectors;

public class BundleParser {


    private BundleParser() {
        // Not used
    }

    public static <E, R extends Resource, C extends FHIRResourceConverter<R, E>> List<E> parse(Class<R> clazz, Bundle bundle, C converter) {
        return bundle
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .filter(entry -> entry.getResource().getResourceType() == ResourceType.fromCode(clazz.getSimpleName()))
                .map(entry -> clazz.cast(entry.getResource()))
                .map(converter::convert)
                .collect(Collectors.toList());
    }
}
