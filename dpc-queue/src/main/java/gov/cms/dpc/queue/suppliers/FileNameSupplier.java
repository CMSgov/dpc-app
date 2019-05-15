package gov.cms.dpc.queue.suppliers;

import org.hl7.fhir.dstu3.model.ResourceType;

import java.util.UUID;

/**
 * Supplier pattern for JobModel::formOutputFileName and JobModel::formErrorFileName
 */
@FunctionalInterface
public interface FileNameSupplier {
    String getFileName(UUID jobID, ResourceType resourceType);
}
