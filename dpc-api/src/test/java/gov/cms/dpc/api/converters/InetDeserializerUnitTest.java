package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.basic.Inet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Inet deserialization")
class InetDeserializerUnitTest {
    InetDeserializer deserializer = new InetDeserializer();
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    JsonParser p;

    @Mock
    DeserializationContext ctx;

    @Test
    @DisplayName("Deserialize inet address 🥳")
    public void deserializeTest_happyPath() throws IOException {
        JsonNode treeNode = mock(JsonNode.class);
        JsonNode addressNode = mock(JsonNode.class);

        when(p.getCodec().readTree(p)).thenReturn(treeNode);
        when(treeNode.get("address")).thenReturn(addressNode);
        when(addressNode.asText()).thenReturn("192.168.1.1");

        Inet response = deserializer.deserialize(p, ctx);
        assertEquals("192.168.1.1", response.getAddress());
    }

    @Test
    @DisplayName("Inet address not found 🤮")
    public void deserializeTest_noIpFound() throws IOException {
        JsonNode treeNode = mock(JsonNode.class);

        when(p.getCodec().readTree(p)).thenReturn(treeNode);
        when(treeNode.get("address")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            deserializer.deserialize(p, ctx);
        });
    }
}
