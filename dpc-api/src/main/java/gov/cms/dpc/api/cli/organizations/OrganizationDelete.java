package gov.cms.dpc.api.cli.organizations;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleBuilder;
import gov.cms.dpc.api.cli.AbstractAttributionCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.cms.dpc.fhir.helpers.FHIRHelpers.getPages;

public class OrganizationDelete extends AbstractAttributionCommand {

    public OrganizationDelete() {
        super("delete", "Delete registered organization");
    }

    @Override
    public void addAdditionalOptions(Subparser subparser) {
        subparser
                .addArgument("id")
                .required(true)
                .dest("org-reference")
                .help("ID of Organization to delete");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) {
        // Get the reference
        final String orgReference = namespace.getString("org-reference");
        System.out.println(String.format("Removing organization %s", orgReference));

        final String attributionService = namespace.getString(ATTR_HOSTNAME);
        System.out.println(String.format("Connecting to Attribution service at: %s", attributionService));

        final IGenericClient client = ctx.newRestfulGenericClient(attributionService);

        // The org and its endpoints must be deleted together since they reference each other
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("organization", Collections.singletonList(orgReference));

        Bundle endpointBundle = client
            .search()
            .forResource(Endpoint.class)
            .encodedJson()
            .returnBundle(Bundle.class)
            .whereMap(searchParams)
            .cacheControl(CacheControlDirective.noCache())
            .execute();
        endpointBundle = getPages(client, endpointBundle);

        // Build a transaction for all of our deletes
        BundleBuilder bundleBuilder = new BundleBuilder(client.getFhirContext());
        bundleBuilder.addTransactionDeleteEntry(new IdType("Organization", orgReference));
        endpointBundle.getEntry().stream()
            .map(entry -> (Endpoint) entry.getResource())
            .forEach(bundleBuilder::addTransactionDeleteEntry);
        client.transaction().withBundle(bundleBuilder.getBundle()).execute();

        System.out.println("Successfully deleted Organization");
    }
}
