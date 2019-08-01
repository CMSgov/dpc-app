package gov.cms.dpc.attribution.utils;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class RESTUtils {

    public static UUID parseTokenTag(String tokenTag) {
        final int idx = tokenTag.indexOf('|');
        if (idx < 0) {
            throw new WebApplicationException("Malformed tokenTag", Response.Status.BAD_REQUEST);
        }

        return UUID.fromString(tokenTag.substring(idx + 1));
    }
}
