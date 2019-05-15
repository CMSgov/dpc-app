package gov.cms.dpc.api.core;


import gov.cms.dpc.common.utils.PropertiesProvider;
import org.hl7.fhir.dstu3.model.*;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hl7.fhir.dstu3.model.CapabilityStatement.*;

public class Capabilities {
    private static final DateTimeFormatter FHIR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Capabilities() {
    }

    public static CapabilityStatement buildCapabilities(String baseUri, String version) {
        final PropertiesProvider pp = new PropertiesProvider();

        DateTimeType releaseDate = DateTimeType.parseV3(pp.getBuildTimestamp().format(FHIR_FORMATTER));

        CapabilityStatement capabilityStatement = new CapabilityStatement();
        capabilityStatement
                .setStatus(Enumerations.PublicationStatus.ACTIVE)
                .setDateElement(releaseDate)
                .setPublisher("Centers for Medicare and Medicaid Services")
                // This should track the FHIR version used by BlueButton
                .setFhirVersion("3.0.1")
                .setSoftware(generateSoftwareComponent(releaseDate, pp.getBuildVersion()))
                .setKind(CapabilityStatementKind.CAPABILITY)
                .setRest(generateRestComponents(baseUri + version))
                .setAcceptUnknown(UnknownContentCode.NO)
                .setFormat(Arrays.asList(new CodeType("application/json"), new CodeType("application/fhir+json")));

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

    private static List<CapabilityStatementRestComponent> generateRestComponents(String baseURI) {
        final CapabilityStatementRestComponent serverComponent = new CapabilityStatementRestComponent();
        serverComponent.setMode(RestfulCapabilityMode.SERVER);

        // Create batch interaction
        final SystemInteractionComponent batchInteraction = new SystemInteractionComponent(new Enumeration<>(new SystemRestfulInteractionEnumFactory(), SystemRestfulInteraction.BATCH));
        serverComponent.setInteraction(Collections.singletonList(batchInteraction));

        // Add the version and metadata endpoints
        final CapabilityStatementRestOperationComponent metadataResource = new CapabilityStatementRestOperationComponent(new StringType("Metadata"), new Reference(String.format("%s/metadata", baseURI)));
        final CapabilityStatementRestOperationComponent versionResource = new CapabilityStatementRestOperationComponent(new StringType("Version"), new Reference(String.format("%s/_version", baseURI)));

        //  Group resources
        final CapabilityStatementRestOperationComponent providerExport = new CapabilityStatementRestOperationComponent(new StringType("Provider export"), new Reference(String.format("%s/Group/providerID/$export", baseURI)));

        // Job resources
        final CapabilityStatementRestOperationComponent jobResource = new CapabilityStatementRestOperationComponent(new StringType("Job Status"), new Reference(String.format("%s/Job/jobID", baseURI)));

        // Roster endpoints
        final CapabilityStatementRestOperationComponent rosterResource = new CapabilityStatementRestOperationComponent(new StringType("Roster submission and updating"), new Reference(String.format("%s/Bundle", baseURI)));

        // Data endpoints
        final CapabilityStatementRestOperationComponent dataResource = new CapabilityStatementRestOperationComponent(new StringType("Export file retrieval"), new Reference(String.format("%s/Data/fileID", baseURI)));

        serverComponent.setOperation(Arrays.asList(
                providerExport,
                metadataResource,
                versionResource,
                jobResource,
                rosterResource,
                dataResource));

        return Collections.singletonList(serverComponent);
    }
}
