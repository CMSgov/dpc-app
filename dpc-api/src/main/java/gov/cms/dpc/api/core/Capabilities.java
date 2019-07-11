package gov.cms.dpc.api.core;


import gov.cms.dpc.common.utils.PropertiesProvider;
import gov.cms.dpc.fhir.FHIRFormatters;
import org.hl7.fhir.dstu3.model.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hl7.fhir.dstu3.model.CapabilityStatement.*;

public class Capabilities {

    private Capabilities() {
    }

    public static CapabilityStatement buildCapabilities() {
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
//                generateGroupEndpoints(),
                generatePractitionerEndpoints(),
                generateStructureDefinitionEndpoints()
        ));

        return Collections.singletonList(serverComponent);
    }

    @SuppressWarnings({"UnusedMethod"}) // Will be expanded with DPC-293
    private static CapabilityStatementRestResourceComponent generateGroupEndpoints() {
        final CapabilityStatementRestResourceComponent group = new CapabilityStatementRestResourceComponent();
        group.setType("Group");

        // STU3 does not support resource level operations, so we'll just add a document comment for now.
        group.setDocumentation("Defines the $export operator, which complies with the draft Bulk Data Specification");

        return group;
    }

    private static CapabilityStatementRestResourceComponent generatePractitionerEndpoints() {
        final CapabilityStatementRestResourceComponent practitioner = new CapabilityStatementRestResourceComponent();
        practitioner.setType("Practitioner");
        practitioner.setVersioning(ResourceVersionPolicy.NOVERSION);

        practitioner.setInteraction(List.of(
                new ResourceInteractionComponent().setCode(TypeRestfulInteraction.CREATE),
                new ResourceInteractionComponent().setCode(TypeRestfulInteraction.UPDATE),
                new ResourceInteractionComponent().setCode(TypeRestfulInteraction.DELETE),
                new ResourceInteractionComponent().setCode(TypeRestfulInteraction.SEARCHTYPE)
        ));

        practitioner.setSearchParam(List.of(
                new CapabilityStatementRestResourceSearchParamComponent().setName("identifier").setType(Enumerations.SearchParamType.STRING)
        ));

        return practitioner;
    }

    private static CapabilityStatementRestResourceComponent generateStructureDefinitionEndpoints() {
        final CapabilityStatementRestResourceComponent definitions = new CapabilityStatementRestResourceComponent();
        definitions.setType("StructureDefinition");
        definitions.setVersioning(ResourceVersionPolicy.NOVERSION);

        definitions.setInteraction(List.of(
                new ResourceInteractionComponent().setCode(TypeRestfulInteraction.READ)
        ));

        return definitions;
    }
}
