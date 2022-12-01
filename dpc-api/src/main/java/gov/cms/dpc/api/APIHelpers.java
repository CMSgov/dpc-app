package gov.cms.dpc.api;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.SingleValidationMessage;
import com.google.common.net.HttpHeaders;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.*;
import java.time.Instant;

import javax.servlet.http.HttpServletRequest;
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
            meta.getTag().removeIf(coding -> DPCIdentifierSystem.DPC.getSystem().equalsIgnoreCase(coding.getSystem().trim()));
            meta.addTag(orgTag);
        }
    }

    /**
     * Fetch the BFD database last update time. Use it as the transactionTime for a job.
     *
     * @return transactionTime from the BFD service
     */
    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR stu3 Meta model
    public static OffsetDateTime fetchTransactionTime(BlueButtonClient bfdClient) {
        // Every bundle has transaction time after the Since RFC has beneficiary
        final Meta meta = bfdClient.requestPatientFromServer(SYNTHETIC_BENE_ID, null, null).getMeta();
        return Optional.ofNullable(meta.getLastUpdated())
                .map(u -> u.toInstant().atOffset(ZoneOffset.UTC))
                .orElse(OffsetDateTime.now(ZoneOffset.UTC));
    }

    public static String fetchRequestingIP(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        //If client uses the forwarded for header, respect that, otherwise use the requester's ip address
        String ipAddress = request.getHeader(HttpHeaders.X_FORWARDED_FOR);
        if (StringUtils.isBlank(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    public static String fetchRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getQueryString() == null ? request.getRequestURL().toString() : request.getRequestURL().append(request.getQueryString()).toString();
    }

    public static String getSplunkTimestamp() {
        return Instant.now().toString().replace("T", " ").substring(0, 22);
    }
}
