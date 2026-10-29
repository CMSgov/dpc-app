package gov.cms.dpc.attribution.utils.stu3;

import gov.cms.dpc.common.hibernate.attribution.DPCAbstractDAO;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.lang3.exception.UncheckedException;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RESTUtilsUnitTest {
	DPCAbstractDAO<?> dao = mock(DPCAbstractDAO.class);
	UnaryOperator<Resource> action = mock(UnaryOperator.class);

	@Test
	void test_bulkResourceHandler_happy_path() {
		RESTUtils.bulkResourceHandler(
			Stream.of(new Patient(), new Patient(), new Patient()), action, dao, 10);

		verify(action, times(3)).apply(any());
		verify(dao, never()).cleanUpBatch();
	}

	@Test
	void test_bulkResourceHandler_empty_stream() {
		RESTUtils.bulkResourceHandler(Stream.of(), action, dao, 10);

		verify(action, never()).apply(any());
	}

	@Test
	void test_bulkResourceHandler_error_on_action() {
		when(action.apply(any())).thenThrow(UncheckedException.class);
		Stream<Resource> resources = Stream.of(new Patient());

		assertThrows(WebApplicationException.class, () ->
			RESTUtils.bulkResourceHandler(resources, action, dao, 10));
	}

	@Test
	void test_bulkResourceHandler_cleans_batch() {
		RESTUtils.bulkResourceHandler(
			Stream.of(new Patient(), new Patient(), new Patient(), new Patient()), action, dao, 2);

		verify(dao, times(2)).cleanUpBatch();
	}
}
