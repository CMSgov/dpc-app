package gov.cms.dpc.api.models;

import org.hl7.fhir.dstu3.model.ResourceType;

/**
 * An entry in the {@link JobCompletionModel} output field.
 */
public class OutputEntryModel {
    /**
     * the FHIR resource type that is contained in the file
     */
    public ResourceType type;

    /**
     * the path to the file
     */
    public String url;

    public OutputEntryModel() {

    }

    public OutputEntryModel(ResourceType type, String url) {
        this.type = type;
        this.url = url;
    }
}
