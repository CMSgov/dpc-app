package gov.cms.dpc.api.converters;

import gov.cms.dpc.api.models.RangeHeader;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ParamConverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ParamConverter} for handling range request values from the {@link org.eclipse.jetty.http.HttpHeader#RANGE} header.
 * This follows the RFC as closely as possible, see <a href="https://tools.ietf.org/html/rfc7233#section-3.1> the IETF RFC</a>
 * One potential deviation (not entirely sure from the IETF docs) is that we simply accept an empty value and ignore it, rather than throwing an exception
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Range">MDN Range Header</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests">MDN Range Request</a>
 */
class HttpRangeHeaderParamConverter implements ParamConverter<RangeHeader> {
    /**
     * {@link Pattern} for extracting Range request (e.g. bytes=0-12345)
     */
    private static final Pattern RANGE_REGEX = Pattern.compile("([a-z]+)=([0-9]+)-([0-9]+)?");
    static final String RANGE_MSG_FORMATTER = "%s is not a valid range request";

    HttpRangeHeaderParamConverter() {
        // not used
    }

    @Override
    public RangeHeader fromString(String value) {
        // If the range request is completely empty, treat it as if it was never set
        if (value == null || value.isEmpty()) {
            return null;
        }
        final Matcher matcher = RANGE_REGEX.matcher(value);
        if (matcher.matches()) {
            final RangeHeader rangeHeader = new RangeHeader();
            rangeHeader.setUnit(matcher.group(1));
            rangeHeader.setStart(Long.parseLong(matcher.group(2)));
            if (matcher.group(3) != null) {
                rangeHeader.setEnd(Long.parseLong(matcher.group(3)));
            }
            return rangeHeader;
        }
        throw new WebApplicationException(String.format(RANGE_MSG_FORMATTER, value), Response.Status.BAD_REQUEST);
    }

    @Override
    public String toString(RangeHeader value) {
        return value.toString();
    }
}
