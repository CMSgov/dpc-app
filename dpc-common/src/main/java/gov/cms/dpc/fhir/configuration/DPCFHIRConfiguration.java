package gov.cms.dpc.fhir.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DPCFHIRConfiguration {

    @JsonProperty("validation")
    private FHIRValidationConfiguration validation;

    public DPCFHIRConfiguration() {
        // Jackson required
    }

    public FHIRValidationConfiguration getValidation() {
        return validation;
    }

    public void setValidation(FHIRValidationConfiguration validation) {
        this.validation = validation;
    }

    public static class FHIRValidationConfiguration {

        private boolean enabled;
        private boolean schemaValidation;
        private boolean schematronValidation;
        private boolean debugValidation;

        public FHIRValidationConfiguration() {
            // Jackson required
        }

        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Enable or disable FHIR resource validation
         *
         * @param enabled - {@code true} validation is enabled. {@code false} validation is disabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isSchemaValidation() {
            return schemaValidation;
        }

        /**
         * Enable or disable FHIR schema validation
         * <p>
         * Note: If enabled, FHIR validation resource JARs must be in classpath.
         *
         * @param schemaValidation - {@code true} validation is enabled. {@code false} validation is disabled
         */
        public void setSchemaValidation(boolean schemaValidation) {
            this.schemaValidation = schemaValidation;
        }

        public boolean isSchematronValidation() {
            return schematronValidation;
        }

        /**
         * Enable or disable Schematron validation
         * <p>
         * Note: If enabled, ph-schematron jar must be in classpath
         *
         * @param schematronValidation - {@code true} validation is enabled. {@code false} validation is disabled
         */
        public void setSchematronValidation(boolean schematronValidation) {
            this.schematronValidation = schematronValidation;
        }

        public boolean isDebugValidation() {
            return debugValidation;
        }

        /**
         * Enable or disable debug validation for generated resources.
         * <p>
         * If enabled, generated FHIR responses are validated before returning.
         *
         * @param debugValidation - {@code true} validation is enabled. {@code false} validation is disabled
         */
        public void setDebugValidation(boolean debugValidation) {
            this.debugValidation = debugValidation;
        }
    }
}
