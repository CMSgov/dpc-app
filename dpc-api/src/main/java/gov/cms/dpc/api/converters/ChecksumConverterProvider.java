package gov.cms.dpc.api.converters;

import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.ClassTypePair;

import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of Jersey's {@link ParamConverterProvider} which processes input values to remove any trailing characters from {@link HttpHeaders#IF_NONE_MATCH} header.
 * <p>
 * This class allows for both raw {@link String} values, as well as Strings wrapped in an {@link Optional}, the logic is borrowed from Dropwizard's optional handling.
 * We can't use their logic directly, because we need to do more advanced matching against header values
 *
 * @see <a href="https://github.com/dropwizard/dropwizard/blob/master/dropwizard-jersey/src/main/java/io/dropwizard/jersey/optional/OptionalParamConverterProvider.java">Dropwizard Optional Handling</a>
 */
@Provider
public class ChecksumConverterProvider implements ParamConverterProvider {

    @Inject
    public ChecksumConverterProvider() {
        // Not used
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {

        // Look to see if we have a matching header
        final boolean headerMatches = Arrays.stream(annotations)
                .filter(annotation -> annotation.annotationType().isAssignableFrom(HeaderParam.class))
                .map(annotation -> ((HeaderParam) annotation).value())
                .anyMatch(value -> value.equals(HttpHeaders.IF_NONE_MATCH));

        if (!headerMatches) {
            return null;
        }

        // Now, check to see if we have an optional value, in which case we have to wrap the result of the param conversion
        if (Optional.class.equals(rawType)) {
            return provideOptionalConverter(rawType, genericType);
        } else if (String.class.equals(rawType)) {
            return (ParamConverter<T>) new ChecksumParamConverter();
        }

        return null;
    }

    private <T> ParamConverter<T> provideOptionalConverter(Class<T> rawType, Type genericType) {
        final List<ClassTypePair> ctps = ReflectionHelper.getTypeArgumentAndClass(genericType);
        final ClassTypePair ctp = (ctps.size() == 1) ? ctps.get(0) : null;

        if (ctp == null || ctp.rawClass() == String.class) {
            return new ParamConverter<>() {
                @Override
                public T fromString(String value) {
                    return rawType.cast(Optional.ofNullable(ChecksumParamConverter.stringMatchLogic(value)));
                }

                @Override
                public String toString(T value) {
                    return value.toString();
                }
            };
        }

        return null;
    }
}
