package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Path("/Group")
@FHIR
public abstract class AbstractGroupResource {

    protected AbstractGroupResource() {
        // Not used
    }

    @POST
    public abstract Response createRoster(Group attributionRoster);

    @GET
    public abstract List<Group> rosterSearch(UUID rosterID, @NotEmpty String organizationID, String providerNPI, String patientID);

    @GET
    @Path("/{rosterID}")
    public abstract Group getRoster(UUID rosterID);

    @GET
    @Path("/{rosterID}/$patients")
    public abstract List<Patient> getAttributedPatients(@NotNull UUID rosterID, boolean activeOnly);

    @PUT
    @Path("/{rosterID}")
    public abstract Group replaceRoster(UUID rosterID, Group groupUpdate);

    @POST
    @Path("/{rosterID}/$add")
    public abstract Group addRosterMembers(UUID rosterID, Group groupUpdate);
    @POST
    @Path("/{rosterID}/$remove")
    public abstract Group removeRosterMembers(UUID rosterID, Group groupUpdate);

    @DELETE
    @Path("/{rosterID}")
    public abstract Response deleteRoster(UUID rosterID);

    public static boolean rosterSizeTooBig(Integer limit, Group... groups) {
        if (groups == null || groups.length == 0 || limit == null || limit == -1) {
            return false;
        }
        long totalMembers = Arrays.stream(groups)
                .filter(Objects::nonNull)
                .map(Group::getMember)
                .flatMap(List::stream)
                .map(Group.GroupMemberComponent::getEntity)
                .map(Reference::getReference)
                .distinct()
                .count();
        return totalMembers > limit;
    }
}
