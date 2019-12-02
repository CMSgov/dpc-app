package gov.cms.dpc.api.converters;

import gov.cms.dpc.api.models.RangeHeader;

import javax.inject.Inject;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Implementation of Jersey's {@link ParamConverterProvider} which allows for constructing {@link RangeHeader} classes from a given header value
 */
@Provider
public class HttpRangeHeaderParamConverterProvider implements ParamConverterProvider {

    @Inject
    public HttpRangeHeaderParamConverterProvider() {
        // Not used
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (genericType.equals(RangeHeader.class)) {
            return (ParamConverter<T>) new HttpRangeHeaderParamConverter();
        }
        return null;
    }

}
