package gov.cms.dpc.core;

import org.hl7.fhir.r4.model.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hl7.fhir.r4.model.CapabilityStatement.*;

public class Capabilities {

    private Capabilities() {
//        Should not use
    }

    public static CapabilityStatement buildCapabilities(String baseUri, String version) {
        DateTimeType releaseDate = DateTimeType.now();

        CapabilityStatement capabilityStatement = new CapabilityStatement();

        capabilityStatement
                .setStatus(Enumerations.PublicationStatus.ACTIVE)
                .setDateElement(releaseDate)
                .setPublisher("Centers for Medicare and Medicaid Services")
                .setFhirVersion("4.0.0")
                .setSoftware(generateSoftwareComponent(releaseDate))
                .setKind(CapabilityStatementKind.CAPABILITY)
                .setRest(generateRestComponents(baseUri + version))
                .setFormat(Arrays.asList(new CodeType("application/json"), new CodeType("application/fhir+json")));

        // Set the narrative
        capabilityStatement.getText().setDivAsString("<div>This is a narrative</div>");
        capabilityStatement.getText().setStatus(Narrative.NarrativeStatus.GENERATED);


        return capabilityStatement;
    }

    private static CapabilityStatementSoftwareComponent generateSoftwareComponent(DateTimeType releaseDate) {
        return new CapabilityStatementSoftwareComponent()
                .setName("Data @ Point of Care API")
                .setVersion("0.0.1")
                .setReleaseDateElement(releaseDate);
    }

    private static List<CapabilityStatementRestComponent> generateRestComponents(String baseURI) {
        final CapabilityStatementRestComponent serverComponent = new CapabilityStatementRestComponent();
        serverComponent.setMode(RestfulCapabilityMode.SERVER);

        // Create batch interaction
        final SystemInteractionComponent batchInteraction = new SystemInteractionComponent(new Enumeration<>(new SystemRestfulInteractionEnumFactory(), SystemRestfulInteraction.BATCH));
        serverComponent.setInteraction(Collections.singletonList(batchInteraction));

        // Add the version and metadata endpoints
        final CapabilityStatementRestResourceOperationComponent metadataResource = new CapabilityStatementRestResourceOperationComponent(new StringType("Metadata"), new CanonicalType(String.format("%s/metadata", baseURI)));
        final CapabilityStatementRestResourceOperationComponent versionResource = new CapabilityStatementRestResourceOperationComponent(new StringType("Version"), new CanonicalType(String.format("%s/_version", baseURI)));

        // Add the Group export paths
        final CapabilityStatementRestResourceOperationComponent providerExport = new CapabilityStatementRestResourceOperationComponent(new StringType("Provider export"), new CanonicalType(String.format("%s/Group/providerID/$export", baseURI)));
        serverComponent.setOperation(Arrays.asList(providerExport, metadataResource, versionResource));

        return Collections.singletonList(serverComponent);
    }
}
