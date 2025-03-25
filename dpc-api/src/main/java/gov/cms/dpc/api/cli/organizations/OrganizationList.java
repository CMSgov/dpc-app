package gov.cms.dpc.api.cli.organizations;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.jakewharton.fliptables.FlipTable;
import gov.cms.dpc.api.cli.AbstractAttributionCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;

import java.util.List;

public class OrganizationList extends AbstractAttributionCommand {

    public OrganizationList() {
        super("list", "List registered organizations");
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {
        // Not used
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) {
        System.out.println("Listing");
        final String attributionService = namespace.getString(ATTR_HOSTNAME);
        System.out.printf("Connecting to Attribution service at: %s%n", attributionService);

        final IGenericClient client = ctx.newRestfulGenericClient(attributionService);

        final Bundle organizations = client
                .search()
                .forResource(Organization.class)
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();

        // Generate the table
        final String[] headers = {"ID", "NPI", "NAME"};

        final List<String[]> collect = organizations
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .map(Organization.class::cast)
                .map(resource ->
                        List.of(resource.getId(), resource.getIdentifierFirstRep().getValue(), resource.getName()))
                .map(values -> values.toArray(new String[0]))
                .toList();

        System.out.println(FlipTable.of(headers, collect.toArray(new String[0][])));
    }
}
