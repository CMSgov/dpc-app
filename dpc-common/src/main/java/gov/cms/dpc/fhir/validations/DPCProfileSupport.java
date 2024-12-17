package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import com.google.inject.Inject;
import gov.cms.dpc.fhir.helpers.ServiceLoaderHelpers;
import gov.cms.dpc.fhir.validations.profiles.IProfileLoader;
import org.hl7.fhir.dstu3.conformance.ProfileUtilities;
import org.hl7.fhir.dstu3.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * DPC specific implementation of FHIR's {@link IValidationSupport}, which allows us to load our own {@link StructureDefinition}s from the JAR.
 * <p>
 * Loading is done through Java's {@link ServiceLoader} feature, we declare profiles that implement {@link IProfileLoader} and then place them in the corresponding file under META-INF/services
 */
public class DPCProfileSupport implements IValidationSupport {

    private static final Logger logger = LoggerFactory.getLogger(DPCProfileSupport.class);

    private final FhirContext ctx;
    private final Map<String, StructureDefinition> structureMap;

    @Inject
    public DPCProfileSupport(FhirContext ctx) {
        this.ctx = ctx;
        this.structureMap = loadProfiles(ctx);
    }

    @Override
    public FhirContext getFhirContext() {
        return ctx;
    }

    @Override
    public List<StructureDefinition> fetchAllStructureDefinitions() {
        return new ArrayList<>(this.structureMap.values());
    }

    @Override
    public <T extends IBaseResource> T fetchResource(Class<T> theClass, String theUri) {
        if (theClass != null && theClass.equals(StructureDefinition.class)) {
            final StructureDefinition definition = this.structureMap.get(theUri);
            if (definition != null) {
                return theClass.cast(definition);
            }
        }

        return null;
    }

    @Override
    public StructureDefinition fetchStructureDefinition(String theUrl) {

        return this.structureMap.get(theUrl);
    }

    @Override
    public boolean isCodeSystemSupported(ValidationSupportContext theContext, String theSystem) {
        return false;
    }

    private Map<String, StructureDefinition> loadProfiles(FhirContext ctx) {

        logger.info("Loading resource profiles");

        final Map<String, StructureDefinition> definitionMap = new HashMap<>();

        // Generate a validator to pull the base definitions from.
        final DefaultProfileValidationSupport defaultValidation = new DefaultProfileValidationSupport(ctx);

        final HapiWorkerContext hapiWorkerContext = new HapiWorkerContext(ctx, defaultValidation);

        final ProfileUtilities profileUtilities = new ProfileUtilities(hapiWorkerContext, new ArrayList<>(), null);

        final IParser parser = ctx.newJsonParser();

        ServiceLoaderHelpers.getLoaderStream(IProfileLoader.class)
                .map(profileLoader -> toStructureDefinition(parser, profileLoader.getPath()))
                .filter(Objects::nonNull)
                .map(diffStructure -> mergeDiff(defaultValidation, profileUtilities, diffStructure))
                .forEach(structure -> definitionMap.put(structure.getUrl(), structure));

        return definitionMap;
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

    private StructureDefinition mergeDiff(DefaultProfileValidationSupport defaultValidation, ProfileUtilities utils, StructureDefinition diffStruct) {
        final StructureDefinition baseStructure = (StructureDefinition) defaultValidation.fetchStructureDefinition(diffStruct.getBaseDefinition());
        if (baseStructure != null) {
            utils.generateSnapshot(baseStructure, diffStruct, "", "");
        }

        return diffStruct;
    }
}
