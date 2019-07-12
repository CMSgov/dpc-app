package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.fhir.validations.profiles.IProfileLoader;
import org.hl7.fhir.dstu3.conformance.ProfileUtilities;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.dstu3.hapi.ctx.IValidationSupport;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.StreamSupport;

/**
 * DPC specific implementation of FHIR's {@link IValidationSupport}, which allows us to load our own {@link StructureDefinition}s from the JAR.
 * <p>
 * Loading is done through Java's {@link ServiceLoader} feature, we declare profiles that implement {@link IProfileLoader} and then place them in the corresponding file under META-INF/services
 */
public class DPCProfileSupport implements IValidationSupport {

    private static final Logger logger = LoggerFactory.getLogger(DPCProfileSupport.class);

    private final Map<String, StructureDefinition> structureMap;

    @Inject
    public DPCProfileSupport(FhirContext ctx) {
        this.structureMap = loadProfiles(ctx);
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

    private Map<String, StructureDefinition> loadProfiles(FhirContext ctx) {

        logger.info("Loading resource profiles");

        final Map<String, StructureDefinition> definitionMap = new HashMap<>();

        // Generate a validator to pull the base definitions from.
        final DefaultProfileValidationSupport defaultValidation = new DefaultProfileValidationSupport();

        final HapiWorkerContext hapiWorkerContext = new HapiWorkerContext(ctx, defaultValidation);

        final ProfileUtilities profileUtilities = new ProfileUtilities(hapiWorkerContext, new ArrayList<>(), null);

        final IParser parser = ctx.newJsonParser();


        final Iterator<IProfileLoader> loader = createLoader();
        Iterable<IProfileLoader> targetStream = () -> loader;

        StreamSupport.stream(targetStream.spliterator(), false)
                .map(profileLoader -> toStructureDefinition(parser, profileLoader.getPath()))
                .filter(Objects::nonNull)
                .map(diffStructure -> mergeDiff(ctx, defaultValidation, profileUtilities, diffStructure))
                .forEach(structure -> definitionMap.put(structure.getUrl(), structure));

        return definitionMap;
    }

    private Iterator<IProfileLoader> createLoader() {
        final ServiceLoader<IProfileLoader> loader = ServiceLoader.load(IProfileLoader.class);
        return loader.iterator();
    }

    private StructureDefinition toStructureDefinition(IParser parser, String structurePath) {
        logger.info("Loading profile: {}", structurePath);
        try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream(structurePath)) {
            if (stream == null) {
                throw new MissingResourceException("Cannot load structure definition", this.getClass().getName(), structurePath);
            }
            return parseStructureDefinition(parser, structurePath, stream);
        } catch (IOException e) {
            throw new IllegalStateException("For some reason, can't read.", e);
        }
    }

    private StructureDefinition parseStructureDefinition(IParser parser, String structurePath, InputStream stream) {
        try {
            return parser.parseResource(StructureDefinition.class, stream);
        } catch (DataFormatException e) {
            logger.error("Unable to parse profile: {}", structurePath, e);
            return null;
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
