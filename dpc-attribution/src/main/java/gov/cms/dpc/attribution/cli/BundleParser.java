package gov.cms.dpc.attribution.cli;

import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.converters.FHIRResourceConverter;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Resource;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BundleParser {


    private BundleParser() {
        // Not used
    }

    public static <E, R extends Resource, C extends FHIRResourceConverter<R, E>> List<E> parse(Class<R> clazz, Bundle bundle, C converter, UUID organizationID) {
        return bundle
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .filter(entry -> entry.getResource().getResourceType().getPath().equals(DPCResourceType.fromCode(clazz.getSimpleName()).getPath()))
                .map(entry -> clazz.cast(entry.getResource()))
                .peek(resource -> {
                    final Meta meta = new Meta();
                    meta.addTag(DPCIdentifierSystem.DPC.getSystem(), organizationID.toString(), "Organization ID");
                    resource.setMeta(meta);
                })
                .map(converter::convert)
                .collect(Collectors.toList());
    }
}
