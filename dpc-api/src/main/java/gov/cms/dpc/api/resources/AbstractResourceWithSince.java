package gov.cms.dpc.api.resources;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;

import javax.ws.rs.BadRequestException;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractResourceWithSince {

  protected AbstractResourceWithSince() {
    // Not used
  }
  
  protected OffsetDateTime handleSinceQueryParam(String sinceParam) {
        if (!StringUtils.isBlank(sinceParam)) {
            try{
                OffsetDateTime sinceDate = OffsetDateTime.parse(sinceParam, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                if (sinceDate.isAfter(OffsetDateTime.now(ZoneId.systemDefault()))) {
                    throw new BadRequestException("'_since' query parameter cannot be a future date");
                }
                return sinceDate;
            }catch (DateTimeParseException e){
                throw new BadRequestException("_since parameter `"+e.getParsedString()+"` could not be parsed at index "+e.getErrorIndex());
            }
        }
        return null;
    }
}