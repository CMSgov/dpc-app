package gov.cms.dpc.common.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.dpc.fhir.DPCResourceType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobCompletionModelUnitTest {

    @Test
    void testOutputEntry() {
        JobCompletionModel.FhirExtension extension = new JobCompletionModel.FhirExtension();
        JobCompletionModel.OutputEntry entry = new JobCompletionModel.OutputEntry(
                DPCResourceType.Bundle,
                "filepath.ndjson",
                10,
                List.of(extension)
        );

        assertAll(
                () -> assertEquals(DPCResourceType.Bundle, entry.getType()),
                () -> assertEquals("filepath.ndjson", entry.getUrl()),
                () -> assertEquals(10, entry.getCount()),
                () -> assertEquals(List.of(extension), entry.getExtension())
        );
    }

    @Test
    void testFhirExtension() {
        JobCompletionModel.FhirExtension stringExtension = new JobCompletionModel.FhirExtension(JobCompletionModel.CHECKSUM_URL, "valueString");
        assertAll(
                () -> assertEquals(JobCompletionModel.CHECKSUM_URL, stringExtension.getUrl()),
                () -> assertEquals("valueString", stringExtension.getValueString())
        );

        JobCompletionModel.FhirExtension decimalExtension = new JobCompletionModel.FhirExtension(JobCompletionModel.FILE_LENGTH_URL, Long.valueOf("1024"));
        assertAll(
                () -> assertEquals(JobCompletionModel.FILE_LENGTH_URL, decimalExtension.getUrl()),
                () -> assertEquals(Long.valueOf("1024"), decimalExtension.getValueDecimal())
        );
        OffsetDateTime dateTime = OffsetDateTime.now();
        JobCompletionModel.FhirExtension dateExtension = new JobCompletionModel.FhirExtension(JobCompletionModel.SUBMIT_TIME_URL, dateTime);
        assertAll(
                () -> assertEquals(JobCompletionModel.SUBMIT_TIME_URL, dateExtension.getUrl()),
                () -> assertEquals(dateTime, dateExtension.getValueDateTime())
        );
    }

    @Test
    void testJobCompletionModel() {
        OffsetDateTime dateTime = OffsetDateTime.now();
        String requestUrl = "http://localhost:8080";
        JobCompletionModel.OutputEntry outputEntry = new JobCompletionModel.OutputEntry();
        JobCompletionModel.OutputEntry errorEntry = new JobCompletionModel.OutputEntry();
        JobCompletionModel.FhirExtension extension = new JobCompletionModel.FhirExtension();
        JobCompletionModel model = new JobCompletionModel(
                dateTime,
                requestUrl,
                List.of(outputEntry),
                List.of(errorEntry),
                List.of(extension)
        );
        assertAll(
                () -> assertEquals(dateTime, model.getTransactionTime()),
                () -> assertEquals(requestUrl, model.getRequest()),
                () -> assertEquals(List.of(outputEntry), model.getOutput()),
                () -> assertEquals(List.of(errorEntry), model.getError()),
                () -> assertEquals(List.of(extension), model.getExtension())
        );
    }

    @Test
    void testDeserialize() throws IOException {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // Load serialized JobCompletionModel
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("JobCompletionModel.json");
        String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        // Try to deserialize JobCompletionModel
        JobCompletionModel jobCompletionModel = mapper.readValue(json, JobCompletionModel.class);
        assertEquals("http://dev.dpc.cms.gov/api/v1/Group/82771a76-f010-44bd-9f7a-7fb02b130f11/$export_format=json", jobCompletionModel.getRequest());
    }
}
