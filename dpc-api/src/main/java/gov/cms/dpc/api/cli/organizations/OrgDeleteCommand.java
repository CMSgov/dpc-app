package gov.cms.dpc.api.cli.organizations;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.cli.AbstractAttributionCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.hl7.fhir.dstu3.model.IdType;

public class OrgDeleteCommand extends AbstractAttributionCommand {

    OrgDeleteCommand() {
        super("delete", "Delete registered organization");
    }

    @Override
    public void configure(Subparser subparser) {
        // Address of the Attribution Service, which handles organization deletion
        subparser
                .addArgument("--host")
                .dest(ATTR_HOSTNAME)
                .setDefault("http://localhost:3500/v1")
                .help("Address of the Attribution Service, which handles organization registration");
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {
        subparser
                .addArgument("id")
                .dest("org-reference")
                .help("ID of Organization to delete");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) {
        // Get the reference
        final String orgReference = namespace.getString("org-reference");
        System.out.println(String.format("Removing organization %s", orgReference));

        final String attributionService = namespace.getString(ATTR_HOSTNAME);

        final IGenericClient client = ctx.newRestfulGenericClient(attributionService);

        client
                .delete()
                .resourceById(new IdType(orgReference))
                .encodedJson()
                .execute();

        System.out.println("Successfully deleted Organization");
    }
}
