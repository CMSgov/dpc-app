package gov.cms.dpc.api.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMultimap;
import gov.cms.dpc.api.models.KeyPairResponse;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.WebApplicationException;
import java.io.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class KeyPairGenerationTests {

    private final GenerateKeyPair task;
    private final ObjectMapper mapper;

    private KeyPairGenerationTests() {
        this.task = new GenerateKeyPair();
        this.mapper = new ObjectMapper();
    }

    @Test
    void checkSuccessful() throws Exception {
        final Map<String, List<String>> map = Map.of("user", List.of("nickrobison-usds"));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            task.execute(map, new PrintWriter(new OutputStreamWriter(bos)));

            final KeyPairResponse response = this.mapper.readValue(new ByteArrayInputStream(bos.toByteArray()), KeyPairResponse.class);
            assertEquals("nickrobison-usds", response.getCreatedBy(), "Should have correct user");
        }
    }

    @Test
    void checkRequiresUsername() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> task.execute(ImmutableMultimap.of(), new PrintWriter(new OutputStreamWriter(bos))));
            assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, exception.getResponse().getStatus(), "Should have bad response"),
                    () -> assertEquals("Must have ID of user generating keypair", exception.getMessage(), "Should show missing user"));
        }
    }
}
