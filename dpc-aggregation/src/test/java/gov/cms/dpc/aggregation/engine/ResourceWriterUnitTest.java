package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.common.utils.GzipUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceWriterUnitTest {
	private final FhirContext fhirContext = FhirContext.forDstu3();
	private final IParser parser = fhirContext.newJsonParser();

	@Test
	void canWriteFile() throws IOException {
		UUID jobId = UUID.randomUUID();
		UUID batchId = UUID.randomUUID();

		OperationsConfig operationsConfig = new OperationsConfig(
			1,
			FileUtils.getTempDirectory().toString(),
			1
		);

		JobQueueBatch jobQueueBatch = mock(JobQueueBatch.class);
		JobQueueBatchFile jobQueueBatchFile = mock(JobQueueBatchFile.class);

		when(jobQueueBatch.addJobQueueFile(DPCResourceType.Patient, 0, 1)).thenReturn(jobQueueBatchFile);
		when(jobQueueBatch.getBatchID()).thenReturn(batchId);
		when(jobQueueBatch.getJobID()).thenReturn(jobId);
		when(jobQueueBatchFile.getCount()).thenReturn(1);

		ResourceWriter resourceWriter = new ResourceWriter(fhirContext, jobQueueBatch, DPCResourceType.Patient, operationsConfig);

		Patient patient = new Patient();
		patient.setId("123");
		resourceWriter.writeBatch(new AtomicInteger(0), List.of(patient));

		String outputPath = ResourceWriter.formOutputFilePath(operationsConfig.getExportPath(), batchId, DPCResourceType.Patient, 0);
		String inputString = GzipUtil.decompress(outputPath);
		Patient readPatient = parser.parseResource(Patient.class, inputString);
		assertEquals(patient.getIdPart(), readPatient.getIdPart());
	}

	@Test
	void canAppendFile() throws IOException {
		UUID jobId = UUID.randomUUID();
		UUID batchId = UUID.randomUUID();

		OperationsConfig operationsConfig = new OperationsConfig(
			1,
			FileUtils.getTempDirectory().toString(),
			1
		);

		JobQueueBatch jobQueueBatch = mock(JobQueueBatch.class);
		JobQueueBatchFile jobQueueBatchFile = mock(JobQueueBatchFile.class);

		when(jobQueueBatch.addJobQueueFile(DPCResourceType.Patient, 0, 1)).thenReturn(jobQueueBatchFile);
		when(jobQueueBatch.getBatchID()).thenReturn(batchId);
		when(jobQueueBatch.getJobID()).thenReturn(jobId);
		when(jobQueueBatchFile.getCount()).thenReturn(0);

		ResourceWriter resourceWriter = new ResourceWriter(fhirContext, jobQueueBatch, DPCResourceType.Patient, operationsConfig);

		Patient patient1 = new Patient();
		patient1.setId("1");
		Patient patient2 = new Patient();
		patient2.setId("2");

		// Write the first patient to a compressed file, then append the second
		resourceWriter.writeBatch(new AtomicInteger(0), List.of(patient1));
		resourceWriter.writeBatch(new AtomicInteger(0), List.of(patient2));

		String outputPath = ResourceWriter.formOutputFilePath(operationsConfig.getExportPath(), batchId, DPCResourceType.Patient, 0);
		String inputString = GzipUtil.decompress(outputPath);
		String[] patients = inputString.split("\\n");

		assertEquals(2, patients.length);
		Patient readPatient1 = parser.parseResource(Patient.class, patients[0]);
		Patient readPatient2 = parser.parseResource(Patient.class, patients[1]);
		assertEquals(patient1.getIdPart(), readPatient1.getIdPart());
		assertEquals(patient2.getIdPart(), readPatient2.getIdPart());
	}
}
