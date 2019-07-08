package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.fhir.validations.definitions.MatchablePatient;
import org.hl7.fhir.dstu3.conformance.ProfileUtilities;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.dstu3.hapi.ctx.IValidationSupport;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.ArrayList;
import java.util.List;

public class ValidationModule implements IValidationSupport {

    private StructureDefinition def;

    ValidationModule() {
        // Not used
    }

    @Override
    public ValueSet.ValueSetExpansionComponent expandValueSet(FhirContext theContext, ValueSet.ConceptSetComponent theInclude) {
        return null;
    }

    @Override
    public List<IBaseResource> fetchAllConformanceResources(FhirContext theContext) {
        return null;
    }

    @Override
    public List<StructureDefinition> fetchAllStructureDefinitions(FhirContext theContext) {

        final StructureDefinition patientDef = MatchablePatient.definition();

        final DefaultProfileValidationSupport defaultValidation = new DefaultProfileValidationSupport();
        final StructureDefinition baseStructure = defaultValidation.fetchStructureDefinition(theContext, patientDef.getBaseDefinition());

        final HapiWorkerContext hapiWorkerContext = new HapiWorkerContext(theContext, defaultValidation);

        final ProfileUtilities profileUtilities = new ProfileUtilities(hapiWorkerContext, new ArrayList<>(), null);

        profileUtilities.generateSnapshot(baseStructure, patientDef, "", "");

        this.def = patientDef;
        return List.of(patientDef);
    }

    @Override
    public CodeSystem fetchCodeSystem(FhirContext theContext, String theSystem) {
        return null;
    }

    @Override
    public <T extends IBaseResource> T fetchResource(FhirContext theContext, Class<T> theClass, String theUri) {
        return theClass.cast(this.def);
    }

    @Override
    public StructureDefinition fetchStructureDefinition(FhirContext theCtx, String theUrl) {

        return this.def;
    }

    @Override
    public boolean isCodeSystemSupported(FhirContext theContext, String theSystem) {
        return false;
    }

    @Override
    public CodeValidationResult validateCode(FhirContext theContext, String theCodeSystem, String theCode, String theDisplay) {
        return null;
    }
}
