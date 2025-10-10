package gov.cms.dpc.api.resources;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.FHIRHeaders;
import gov.cms.dpc.queue.models.JobQueueBatch;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.QueryParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.hl7.fhir.dstu3.model.Organization;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static gov.cms.dpc.fhir.FHIRMediaTypes.*;

public abstract class AbstractResourceWithExport {
    static final String LIST_DELIMITER = ",";

    protected final IGenericClient client;

    protected AbstractResourceWithExport(IGenericClient client) {
        this.client = client;
    }

    protected OffsetDateTime handleSinceQueryParam(String sinceParam) {
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
    protected List<DPCResourceType> handleTypeQueryParam(String resourcesListParam) {
        // If the query param is omitted, the FHIR spec states that all resources should be returned
        if (resourcesListParam == null || resourcesListParam.isEmpty()) {
            return JobQueueBatch.validResourceTypes;
        }

        final var resources = new ArrayList<DPCResourceType>();
        for (String queryResource : resourcesListParam.split(LIST_DELIMITER, -1)) {
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
    protected static void checkExportRequest(String outputFormat, String headerPrefer) {
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
     * Extracts the {@link Organization} and gets its NPI.
     * @param organizationPrincipal Passed in the request.
     * @return NPI
     */
    protected String getOrgNPI(OrganizationPrincipal organizationPrincipal) {
        final UUID orgId = organizationPrincipal.getID();
        final Organization org = this.client
            .read()
            .resource(Organization.class)
            .withId(orgId.toString())
            .encodedJson()
            .execute();
        return FHIRExtractors.findMatchingIdentifier(org.getIdentifier(), DPCIdentifierSystem.NPPES).getValue();
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
