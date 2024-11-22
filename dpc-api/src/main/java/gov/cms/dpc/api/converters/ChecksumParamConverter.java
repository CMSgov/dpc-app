package gov.cms.dpc.api.converters;

import jakarta.ws.rs.ext.ParamConverter;
import java.util.regex.Pattern;

/**
 * {@link ParamConverter} which processes {@link jakarta.ws.rs.core.HttpHeaders#IF_NONE_MATCH} values to remove any trailing compression values, which cannot be directly matched against the actual checksum.
 * Per the HTTP spec, if a server compresses the response, a matching suffix is added to the {@link jakarta.ws.rs.core.HttpHeaders#ETAG} header.
 * This needs to be removed in order for a 'weak matching' to occur.
 *
 * @see <a href="https://github.com/gocd/gocd/pull/2759/files">GO-CD PR where I borrowed this implementation</a>
 */
public class ChecksumParamConverter implements ParamConverter<String> {

    private static final Pattern MATCH_REGEX = Pattern.compile("--(gzip|deflate)\"$");
    private static final Pattern REPLACE_REGEX = Pattern.compile("--(gzip|deflate)?$");

    ChecksumParamConverter() {
        // Not used
    }

    @Override
    public String fromString(String value) {
        return stringMatchLogic(value);
    }

    static String stringMatchLogic(String value) {
        if (value == null) {
            return null;
        }

        if (MATCH_REGEX.matcher(value).groupCount() == 1) {
            return REPLACE_REGEX.matcher(value).replaceAll("");
        }

        return value;
    }

    @Override
    public String toString(String value) {
        return value;
    }
}
