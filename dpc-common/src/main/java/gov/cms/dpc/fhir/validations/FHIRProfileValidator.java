package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.dstu3.conformance.ProfileUtilities;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.dstu3.hapi.ctx.IValidationSupport;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class FHIRProfileValidator implements IValidationSupport {

    private final Map<String, StructureDefinition> structureMap;

    @Inject
    public FHIRProfileValidator(FhirContext ctx) {
        try {
            this.structureMap = parseBundledDefinitions(ctx);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read structure definitions", e);
        }
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
        return new ArrayList<>(this.structureMap.values());
    }

    @Override
    public CodeSystem fetchCodeSystem(FhirContext theContext, String theSystem) {
        return null;
    }

    @Override
    public <T extends IBaseResource> T fetchResource(FhirContext theContext, Class<T> theClass, String theUri) {
        if (theClass.equals(StructureDefinition.class)) {
            final StructureDefinition definition = this.structureMap.get(theUri);
            if (definition != null) {
                return theClass.cast(definition);
            }
        }

        return null;
    }

    @Override
    public StructureDefinition fetchStructureDefinition(FhirContext theCtx, String theUrl) {

        return this.structureMap.get(theUrl);
    }

    @Override
    public boolean isCodeSystemSupported(FhirContext theContext, String theSystem) {
        return false;
    }

    @Override
    public CodeValidationResult validateCode(FhirContext theContext, String theCodeSystem, String theCode, String theDisplay) {
        return null;
    }

    private Map<String, StructureDefinition> parseBundledDefinitions(FhirContext ctx) throws IOException {

        final Map<String, StructureDefinition> definitionMap = new HashMap<>();

        // Generate a validator to pull the base definitions from.
        final DefaultProfileValidationSupport defaultValidation = new DefaultProfileValidationSupport();

        final HapiWorkerContext hapiWorkerContext = new HapiWorkerContext(ctx, defaultValidation);

        final ProfileUtilities profileUtilities = new ProfileUtilities(hapiWorkerContext, new ArrayList<>(), null);

        final IParser parser = ctx.newJsonParser();
        final String prefix = "validations/";
        getResourceList(prefix)
                .stream()
                .map(resourceName -> toStructureDefinition(parser, prefix + resourceName))
                .map(diffStructure -> mergeDiff(ctx, defaultValidation, profileUtilities, diffStructure))
                .forEach(structure -> definitionMap.put(structure.getUrl(), structure));

        return definitionMap;
    }

    private List<String> getResourceList(String name) throws IOException {
        final List<String> filenames = new ArrayList<>();
        try (final InputStream pathStream = getClass().getClassLoader().getResourceAsStream(name);
             final BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(pathStream)))) {
            String resource;
            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }

        return filenames;
    }

    private StructureDefinition toStructureDefinition(IParser parser, String structurePath) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(structurePath)) {
            if (stream == null) {
                throw new MissingResourceException("Cannot load structure definition", getClass().getName(), structurePath);
            }
            return parser.parseResource(StructureDefinition.class, stream);
        } catch (IOException e) {
            throw new IllegalStateException("For some reason, can't read.", e);
        }
    }

    private StructureDefinition mergeDiff(FhirContext ctx, DefaultProfileValidationSupport defaultValidation, ProfileUtilities utils, StructureDefinition diffStruct) {
        final StructureDefinition baseStructure = defaultValidation.fetchStructureDefinition(ctx, diffStruct.getBaseDefinition());
        if (baseStructure != null) {
            utils.generateSnapshot(baseStructure, diffStruct, "", "");
        }

        return diffStruct;
    }

}
