package gov.cms.dpc.api.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.jakewharton.fliptables.FlipTable;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;

import java.util.List;
import java.util.stream.Collectors;

public class OrgListCommand extends Command {

    private static final String ATTR_HOSTNAME = "hostname";
    private final FhirContext ctx;

    OrgListCommand() {
        super("list", "List registered organizations");
        this.ctx = FhirContext.forDstu3();
        // Disable server validation, since the Attribution Service doesn't have a capabilities statement
        this.ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    }


    @Override
    public void configure(Subparser subparser) {

        // Address of the Attribution Service, which handles organization registration
        subparser
                .addArgument("--host")
                .dest(ATTR_HOSTNAME)
                .setDefault("http://localhost:3500/v1")
                .help("Address of the Attribution Service, which handles organization registration");

    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) {
        System.out.println("Listing");
        final String attributionService = namespace.getString(ATTR_HOSTNAME);

        final IGenericClient client = ctx.newRestfulGenericClient(attributionService);

        final Bundle organizations = client
                .search()
                .forResource(Organization.class)
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();

        // Generate the table
        final String[] headers = {"ID", "NPI", "NAME"};
        String[][] data;

        final List<String[]> collect = organizations
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .map(resource -> (Organization) resource)
                .map(resource ->
                        List.of(resource.getId(), resource.getIdentifierFirstRep().getValue(), resource.getName()))
                .map(values -> values.toArray(new String[0]))
                .collect(Collectors.toList());

        System.out.println(FlipTable.of(headers, collect.toArray(new String[0][])));
    }
}
