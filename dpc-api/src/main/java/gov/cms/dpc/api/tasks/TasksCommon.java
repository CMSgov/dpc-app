package gov.cms.dpc.api.tasks;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import org.hl7.fhir.dstu3.model.Organization;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class TasksCommon {

    private static final String ORG_PARAM = "organization";

    private TasksCommon() {
        // Not used
    }

    /**
     * Extract an {@link Organization} from the Task parameters. Specifically from the `organization` query param.
     *
     * @param parameters - {@link ImmutableMultimap} of parameters
     * @return - {@link Organization} extracted from query params
     * @throws WebApplicationException if the organization ID is missing
     */
    public static Organization extractOrganization(ImmutableMultimap<String, String> parameters) {
        final ImmutableCollection<String> organizationCollection = parameters.get(ORG_PARAM);

        if (organizationCollection.isEmpty()) {
            throw new WebApplicationException("Must have organization", Response.Status.BAD_REQUEST);
        }

        final String organizationID = organizationCollection.asList().get(0);
        final Organization orgResource = new Organization();
        orgResource.setId(organizationID);

        return orgResource;
    }
}
