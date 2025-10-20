package gov.cms.dpc.api.resources;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRHeaders;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.queue.models.JobQueueBatch;
import jakarta.ws.rs.BadRequestException;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class AbstractResourceWithExportUnitTest {
	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	IGenericClient client;

	ResourceWithExport resourceWithExport;

	@BeforeEach
	void setUp() {
		openMocks(this);
		resourceWithExport = new ResourceWithExport(client);
	}

	@Test
	void handleSinceQueryParam_works() {
		String dateTimeString = "2011-12-03T10:15:30+01:00";
		assertEquals(OffsetDateTime.parse(dateTimeString), resourceWithExport.handleSinceQueryParam(dateTimeString));
	}

	@Test
	void handleSinceQueryParam_handlesBlank() {
		String dateTimeString = "";
		assertNull(resourceWithExport.handleSinceQueryParam(dateTimeString));
	}

	@Test
	void handleSinceQueryParam_handlesFutureDate() {
		String dateTimeString = "2999-12-03T10:15:30+01:00";
		assertThrows(BadRequestException.class, () -> resourceWithExport.handleSinceQueryParam(dateTimeString));
	}

	@Test
	void handleSinceQueryParam_handlesUnparseableDate() {
		String dateTimeString = "BadDate";
		assertThrows(BadRequestException.class, () -> resourceWithExport.handleSinceQueryParam(dateTimeString));
	}

	@Test
	void handleTypeQueryParam_returnsAllOnEmptyList() {
		assertEquals(JobQueueBatch.validResourceTypes, resourceWithExport.handleTypeQueryParam(null));
		assertEquals(JobQueueBatch.validResourceTypes, resourceWithExport.handleTypeQueryParam(""));
	}

	@Test
	void handleTypeQueryParam_returnsCorrectResources() {
		String resourceString = String.format("%s,%s", DPCResourceType.Patient, DPCResourceType.Coverage);
		List<DPCResourceType> resourceList = List.of(DPCResourceType.Patient, DPCResourceType.Coverage);
		assertEquals(resourceList, resourceWithExport.handleTypeQueryParam(resourceString));
	}

	@Test
	void handleTypeQueryParam_handlesBadResource() {
		assertThrows(BadRequestException.class, () -> resourceWithExport.handleTypeQueryParam("fake"));
	}

	@Test
	void checkExportRequest_works() {
		assertDoesNotThrow(() -> AbstractResourceWithExport.checkExportRequest(FHIRMediaTypes.FHIR_NDJSON, FHIRHeaders.PREFER_RESPOND_ASYNC));
	}

	@Test
	void checkExportRequest_handlesBadOutputFormat() {
		assertThrows(BadRequestException.class, () -> AbstractResourceWithExport.checkExportRequest("Bad", FHIRHeaders.PREFER_RESPOND_ASYNC));
	}

	@Test
	void checkExportRequest_handlesBlankPreferHeader() {
		assertThrows(BadRequestException.class, () -> AbstractResourceWithExport.checkExportRequest(FHIRMediaTypes.FHIR_NDJSON, null));
		assertThrows(BadRequestException.class, () -> AbstractResourceWithExport.checkExportRequest(FHIRMediaTypes.FHIR_NDJSON, ""));
	}

	@Test
	void checkExportRequest_handlesBadPreferHeader() {
		assertThrows(BadRequestException.class, () -> AbstractResourceWithExport.checkExportRequest(FHIRMediaTypes.FHIR_NDJSON, "Bad"));
	}

	@Test
	void getOrganizationNPI_works() {
		OrganizationPrincipal organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();
		Organization org = organizationPrincipal.getOrganization();

		IReadExecutable<Organization> readExec = mock(IReadExecutable.class);
		when(client.read().resource(Organization.class).withId(org.getId()).encodedJson()).thenReturn(readExec);
		when(readExec.execute()).thenReturn(org);

		assertEquals(APITestHelpers.ORGANIZATION_NPI, resourceWithExport.getOrganizationNPI(organizationPrincipal));
	}

	/**
	 * Implementation of {@link AbstractResourceWithExport} so that we can test its methods here instead of in each
	 * of its subclasses.
	 */
	static class ResourceWithExport extends AbstractResourceWithExport {
		protected ResourceWithExport(IGenericClient client) {
			super(client);
		}
	}
}

