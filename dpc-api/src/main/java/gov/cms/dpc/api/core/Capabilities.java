package gov.cms.dpc.api.core;


import gov.cms.dpc.common.utils.PropertiesProvider;
import gov.cms.dpc.fhir.FHIRFormatters;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hl7.fhir.dstu3.model.CapabilityStatement.*;

public class Capabilities {

    private static final Logger logger = LoggerFactory.getLogger(Capabilities.class);

    private static final Object lock = new Object();
    private static volatile CapabilityStatement statement;

    private Capabilities() {
    }

    /**
     * Get the system's {@link CapabilityStatement}.
     * <p>
     * This value is lazily generated the first time it's called.
     *
     * @return - {@link CapabilityStatement} of system.
     */
    public static CapabilityStatement getCapabilities() {
        // Double lock check to lazy init capabilities statement
        if (statement == null) {
            synchronized (lock) {
                if (statement == null) {
                    logger.debug("Building capabilities statement");
                    statement = buildCapabilities();
                    return statement;
                }
            }
        }
        logger.trace("Returning cached capabilities statement");
        return statement;
    }


    private static CapabilityStatement buildCapabilities() {
        final PropertiesProvider pp = new PropertiesProvider();

        DateTimeType releaseDate = DateTimeType.parseV3(pp.getBuildTimestamp().format(FHIRFormatters.DATE_TIME_FORMATTER));

        CapabilityStatement capabilityStatement = new CapabilityStatement();
        capabilityStatement
                .setStatus(Enumerations.PublicationStatus.DRAFT)
                .setDateElement(releaseDate)
                .setPublisher("Centers for Medicare and Medicaid Services")
                .setVersion(pp.getBuildVersion())
                // This should track the FHIR version used by BlueButton
                .setFhirVersion("3.0.1")
                .setSoftware(generateSoftwareComponent(releaseDate, pp.getBuildVersion()))
                .setKind(CapabilityStatementKind.CAPABILITY)
                .setRest(generateRestComponents())
                .setFormat(Arrays.asList(new CodeType("application/json"), new CodeType("application/fhir+json")))
                .setAcceptUnknown(UnknownContentCode.EXTENSIONS);

        // Set the narrative
        capabilityStatement.getText().setDivAsString("<div>This is a narrative</div>");
        capabilityStatement.getText().setStatus(Narrative.NarrativeStatus.GENERATED);


        return capabilityStatement;
    }

    private static CapabilityStatementSoftwareComponent generateSoftwareComponent(DateTimeType releaseDate, String releaseVersion) {
        return new CapabilityStatementSoftwareComponent()
                .setName("Data @ Point of Care API")
                .setVersion(releaseVersion)
                .setReleaseDateElement(releaseDate);
    }

    private static List<CapabilityStatementRestComponent> generateRestComponents() {
        final CapabilityStatementRestComponent serverComponent = new CapabilityStatementRestComponent();
        serverComponent.setMode(RestfulCapabilityMode.SERVER);

        // Create batch interaction
        final SystemInteractionComponent batchInteraction = new SystemInteractionComponent(new Enumeration<>(new SystemRestfulInteractionEnumFactory(), SystemRestfulInteraction.BATCH));
        serverComponent.setInteraction(Collections.singletonList(batchInteraction));

        serverComponent.setResource(List.of(
                generateRestComponent("Endpoint", List.of(
                        new ResourceInteractionComponent().setCode(TypeRestfulInteraction.READ),
                        new ResourceInteractionComponent().setCode(TypeRestfulInteraction.SEARCHTYPE)
                ), Collections.emptyList()),
                generateRestComponent("Organization", List.of(
                        new ResourceInteractionComponent().setCode(TypeRestfulInteraction.READ)
                ), Collections.emptyList()),
                generateRestComponent("Practitioner", List.of(
                        new ResourceInteractionComponent().setCode(TypeRestfulInteraction.READ),
                        new ResourceInteractionComponent().setCode(TypeRestfulInteraction.CREATE),
                        new ResourceInteractionComponent().setCode(TypeRestfulInteraction.UPDATE),
                        new ResourceInteractionComponent().setCode(TypeRestfulInteraction.DELETE),
                        new ResourceInteractionComponent().setCode(TypeRestfulInteraction.SEARCHTYPE)
                ), List.of(
                        new CapabilityStatementRestResourceSearchParamComponent().setName("identifier").setType(Enumerations.SearchParamType.STRING)
                )),
                generateRestComponent("StructureDefinition", List.of(
                        new ResourceInteractionComponent().setCode(TypeRestfulInteraction.READ))
                        , Collections.emptyList())
        ));

        return Collections.singletonList(serverComponent);
    }

    private static CapabilityStatementRestResourceComponent generateRestComponent(String name, List<ResourceInteractionComponent> interactions, List<CapabilityStatementRestResourceSearchParamComponent> searchParams) {
        final CapabilityStatementRestResourceComponent definitions = new CapabilityStatementRestResourceComponent();
        definitions.setType(name);
        definitions.setVersioning(ResourceVersionPolicy.NOVERSION);

        definitions.setInteraction(interactions);

        if (!searchParams.isEmpty()) {
            definitions.setSearchParam(searchParams);
        }

        return definitions;
    }
}
