package gov.cms.dpc.common.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobCompletionModelUnitTest {
    @Test
    public void testDeserialize() throws IOException {
        Package pack = JsonFormat.class.getPackage();
        System.out.println(pack);
        System.out.println(JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS.toString());

        JsonFormat.Feature readDateTimestampsAsNanoseconds = JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS;

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // Load serialized JobCompletionModel
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("JobCompletionModel.json");
        String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        // Try to deserialize JobCompletionModel
        JobCompletionModel jobCompletionModel = mapper.readValue(json, JobCompletionModel.class);
        assertEquals("http://dev.dpc.cms.gov/api/v1/Group/82771a76-f010-44bd-9f7a-7fb02b130f11/$export_format=json", jobCompletionModel.getRequest());
    }
}
