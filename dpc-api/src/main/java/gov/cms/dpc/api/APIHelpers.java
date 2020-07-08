package gov.cms.dpc.api;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.SingleValidationMessage;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class APIHelpers {

    static final String SYNTHETIC_BENE_ID = "-19990000000001";

    private APIHelpers() {
        // Not used
    }


    public static <T extends BaseResource> Bundle bulkResourceClient(Class<T> clazz, IGenericClient client, Consumer<T> entryConsumer, Bundle resourceBundle) {
        // We need to figure out how to validate the bundle entries
        resourceBundle
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getClass().equals(clazz))
                .map(clazz::cast)
                .forEach(entryConsumer);

        final Parameters params = new Parameters();
        params.addParameter().setResource(resourceBundle);
        return client
                .operation()
                .onType(clazz)
                .named("submit")
                .withParameters(params)
                .returnResourceType(Bundle.class)
                .encodedJson()
                .execute();
    }

    public static String formatValidationMessages(List<SingleValidationMessage> messages) {
        return messages
                .stream()
                .map(SingleValidationMessage::getMessage)
                .collect(Collectors.joining(", "));
    }

    public static void addOrganizationTag(Resource resource, String organizationID) {
        final Coding orgTag = new Coding(DPCIdentifierSystem.DPC.getSystem(), organizationID, "Organization ID");
        final Meta meta = resource.getMeta();
        // If no Meta, create new values
        if (meta == null) {
            final Meta newMeta = new Meta();
            newMeta.addTag(orgTag);
            resource.setMeta(newMeta);
        } else {
            meta.addTag(orgTag);
        }
    }

    /**
     * Fetch the BFD database last update time. Use it as the transactionTime for a job.
     * @return transactionTime from the BFD service
     */
    public static OffsetDateTime fetchTransactionTime(BlueButtonClient bfdClient) {
        // Every bundle has transaction time after the Since RFC has beneficiary
        final Meta meta = bfdClient.requestPatientFromServer(SYNTHETIC_BENE_ID, null).getMeta();
        return Optional.ofNullable(meta.getLastUpdated())
                .map(u -> u.toInstant().atOffset(ZoneOffset.UTC))
                .orElse(OffsetDateTime.now(ZoneOffset.UTC));
    }
}
