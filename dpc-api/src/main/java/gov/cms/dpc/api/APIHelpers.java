package gov.cms.dpc.api;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.SingleValidationMessage;
import com.google.common.net.HttpHeaders;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRHeaders;
import gov.cms.dpc.fhir.stu3.FHIRExtractors;
import gov.cms.dpc.queue.models.JobQueueBatch;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.hl7.fhir.dstu3.model.*;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static gov.cms.dpc.fhir.FHIRMediaTypes.*;

public class APIHelpers {

    static final String SYNTHETIC_BENE_ID = "-19990000000001";

    private APIHelpers() {
        // Not used
    }


    public static <T extends BaseResource> Bundle bulkResourceClient(
        Class<T> clazz,
        IGenericClient client,
        Function<T, Optional<WebApplicationException>> entryFunction,
        Bundle resourceBundle)
    {
        AtomicReference<WebApplicationException> atomicWae = new AtomicReference<>();

        resourceBundle
            .getEntry()
            .parallelStream()
            .filter(Bundle.BundleEntryComponent::hasResource)
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource.getClass().equals(clazz))
            .map(clazz::cast)
            .forEach(entry -> entryFunction.apply(entry).ifPresent(atomicWae::set));

        // If any of our threads returned a WebApplicationException, throw it to the caller.
        if( atomicWae.get() != null ) {
            throw atomicWae.get();
        }

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

    public static void addOrganizationTag(org.hl7.fhir.r4.model.Resource resource, String organizationID) {
        final org.hl7.fhir.r4.model.Coding orgTag = new org.hl7.fhir.r4.model.Coding(DPCIdentifierSystem.DPC.getSystem(), organizationID, "Organization ID");
        final org.hl7.fhir.r4.model.Meta meta = resource.getMeta();
        if (meta == null) {
            final org.hl7.fhir.r4.model.Meta newMeta = new org.hl7.fhir.r4.model.Meta();
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

    /**
     * Fetch an Organization's NPI from the attribution service.
     *
     * @param client - attribution {@link IGenericClient}
     * @param orgId  - {@link UUID} of the organization to look up
     * @return NPI
     */
    public static String fetchOrganizationNPI(IGenericClient client, UUID orgId) {
        final Organization org = client
                .read()
                .resource(Organization.class)
                .withId(orgId.toString())
                .encodedJson()
                .execute();
        return FHIRExtractors.findMatchingIdentifier(org.getIdentifier(), DPCIdentifierSystem.NPPES).getValue();
    }

    public static Optional<String> getOrganizationNPI(OrganizationPrincipal organizationPrincipal) {
        if (organizationPrincipal == null) {
            return Optional.empty();
        }

        Organization organization = organizationPrincipal.getOrganization();
        if (organization == null || !organization.hasIdentifier()) {
            return Optional.empty();
        }

        return organization.getIdentifier().stream()
                .filter(id -> DPCIdentifierSystem.NPPES.getSystem().equalsIgnoreCase(id.getSystem()))
                .map(Identifier::getValue)
                .findFirst();
    }

    public static OffsetDateTime handleSinceQueryParam(String sinceParam) {
        if (!StringUtils.isBlank(sinceParam)) {
            try{
                OffsetDateTime sinceDate = OffsetDateTime.parse(sinceParam, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                if (sinceDate.isAfter(OffsetDateTime.now(ZoneId.systemDefault()))) {
                    throw new BadRequestException("'_since' query parameter cannot be a future date");
                }
                return sinceDate;
            } catch (DateTimeParseException e) {
                throw new BadRequestException("_since parameter `"+e.getParsedString()+"` could not be parsed at index "+e.getErrorIndex());
            }
        }
        return null;
    }

    /**
     * Convert the '_types' {@link QueryParam} to a list of resources to add to the job. Handle the empty case,
     * by returning all valid resource types.
     *
     * @param resourcesListParam - {@link String} of comma separated values corresponding to FHIR {@link DPCResourceType}s
     * @return - A list of {@link DPCResourceType} to return for this request.
     */
    public static List<DPCResourceType> handleTypeQueryParam(String resourcesListParam) {
        // If the query param is omitted, the FHIR spec states that all resources should be returned
        if (resourcesListParam == null || resourcesListParam.isEmpty()) {
            return JobQueueBatch.validResourceTypes;
        }

        final var resources = new ArrayList<DPCResourceType>();
        for (String queryResource : resourcesListParam.split(",", -1)) {
            final var foundResourceType = matchResourceType(queryResource);
            if (foundResourceType.isEmpty()) {
                throw new BadRequestException(String.format("Unsupported resource name in the '_type' query parameter: %s", queryResource));
            }
            resources.add(foundResourceType.get());
        }
        return resources;
    }

    /**
     * Check the query parameters of the request. If not valid, throw a {@link BadRequestException}.
     *
     * @param outputFormat param to check
     */
    public static void checkExportRequest(String outputFormat, String headerPrefer) {
        // _outputFormat only supports FHIR_NDJSON, APPLICATION_NDJSON, NDJSON
        if (!Strings.CI.equalsAny(outputFormat, FHIR_NDJSON, APPLICATION_NDJSON, NDJSON)) {
            throw new BadRequestException("'_outputFormat' query parameter must be '" + FHIR_NDJSON + "', '" + APPLICATION_NDJSON + "', or '" + NDJSON +"' ");
        }
        if (headerPrefer == null || StringUtils.isEmpty(headerPrefer)) {
            throw new BadRequestException("The 'Prefer' header must be '" + FHIRHeaders.PREFER_RESPOND_ASYNC + "'");
        }
        if (!headerPrefer.equals(FHIRHeaders.PREFER_RESPOND_ASYNC)) {
            throw new BadRequestException("The 'Prefer' header must be '" + FHIRHeaders.PREFER_RESPOND_ASYNC + "'");
        }
    }

    /**
     * Convert a single resource type in a query param into a {@link DPCResourceType}.
     *
     * @param queryResourceType - The text from the query param
     * @return If match is found a {@link DPCResourceType}
     */
    private static Optional<DPCResourceType> matchResourceType(String queryResourceType) {
        final var canonical = queryResourceType.trim().toUpperCase();
        // Implementation Note: resourceTypeMap is a small list <3 so hashing isn't faster
        return JobQueueBatch.validResourceTypes.stream()
                .filter(validResource -> validResource.toString().equalsIgnoreCase(canonical))
                .findFirst();
    }
}
