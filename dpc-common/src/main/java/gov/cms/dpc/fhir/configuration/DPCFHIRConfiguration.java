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
