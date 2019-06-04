package gov.cms.dpc.api.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.hl7.fhir.dstu3.model.Organization;

import java.io.File;
import java.io.FileInputStream;

public class OrgRegistrationCommand extends Command {
    private static final String ORG_FILE = "org-file";
    private static final String ATTR_HOSTNAME = "hostname";
    private final FhirContext ctx;

    public OrgRegistrationCommand() {
        super("register", "Register Organization");
        this.ctx = FhirContext.forDstu3();
        // Disable server validation, since the Attribution Service doesn't have a capabilities statement
        this.ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    }

    @Override
    public void configure(Subparser subparser) {
        // Location of FHIR organization file
        subparser
                .addArgument("-f", "--file")
                .dest(ORG_FILE)
                .type(String.class)
                .help("FHIR Organization resource to register with system");

        // Address of the Attribution Service, which handles organization registration
        subparser
                .addArgument("--host")
                .dest(ATTR_HOSTNAME)
                .setDefault("http://localhost:3041/v1")
                .help("Address of the Attribution Service, which handles organization registration");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        System.out.println("Registering Organization");

        // Read the file and parser it
        Organization organization;
        try (FileInputStream fileInputStream = new FileInputStream(new File(namespace.getString(ORG_FILE)))) {
            final IParser parser = ctx.newJsonParser();
            organization = (Organization) parser.parseResource(fileInputStream);
        }

        registerOrganization(organization, namespace.getString(ATTR_HOSTNAME));
    }

    void registerOrganization(Organization organization, String attributionService) {
        final IGenericClient client = ctx.newRestfulGenericClient(attributionService);

        try {
            client
                    .create()
                    .resource(organization)
                    .encodedJson()
                    .execute();
        } catch (Exception e) {
            System.err.println(String.format("Unable to register organization. %s", e.getMessage()));
        }
    }
}
