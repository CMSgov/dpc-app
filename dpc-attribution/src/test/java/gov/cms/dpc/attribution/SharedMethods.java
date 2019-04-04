package gov.cms.dpc.attribution;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;

import java.io.IOException;
import java.util.List;

public class SharedMethods {

    @SuppressWarnings("unchecked")
    public static <T> List<T> UnmarshallResponse(HttpEntity entity) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        return (List<T>) mapper.readValue(entity.getContent(), List.class);
    }
}
