package gov.cms.dpc.attribution.utils;

import gov.cms.dpc.common.hibernate.attribution.DPCAbstractDAO;
import org.apache.commons.lang3.exception.UncheckedException;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class RESTUtilsUnitTest {
	DPCAbstractDAO<?> dao = mock(DPCAbstractDAO.class);
	UnaryOperator<Resource> action = mock(UnaryOperator.class);

	@Test
	void test_bulkResourceHandler_happy_path() {
		RESTUtils.bulkResourceHandler(
			Stream.of(new Patient(), new Patient(), new Patient()), action, dao, 10);

		Mockito.verify(action, Mockito.times(3)).apply(any());
		Mockito.verify(dao, Mockito.never()).cleanUpBatch();
	}

	@Test
	void test_bulkResourceHandler_empty_stream() {
		RESTUtils.bulkResourceHandler(Stream.of(), action, dao, 10);

		Mockito.verify(action, Mockito.never()).apply(any());
	}

	@Test
	void test_bulkResourceHandler_error_on_action() {
		Mockito.when(action.apply(any())).thenThrow(UncheckedException.class);

		assertThrows(WebApplicationException.class, () ->
			RESTUtils.bulkResourceHandler(Stream.of(new Patient()), action, dao, 10));
	}

	@Test
	void test_bulkResourceHandler_cleans_batch() {
		RESTUtils.bulkResourceHandler(
			Stream.of(new Patient(), new Patient(), new Patient(), new Patient()), action, dao, 2);

		Mockito.verify(dao, Mockito.times(2)).cleanUpBatch();
	}
}
