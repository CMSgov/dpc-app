package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import com.github.nitram509.jmacaroons.Macaroon;
import com.github.nitram509.jmacaroons.MacaroonVersion;

import java.util.List;
import java.util.stream.Collectors;

public class MacaroonListToJSONConverter extends StdConverter<List<Macaroon>, String> {

    MacaroonListToJSONConverter() {
        // Jackson required
    }

    @Override
    public String convert(List<Macaroon> value) {
        return value
                .stream()
                .map(m -> m.serialize(MacaroonVersion.SerializationVersion.V2_JSON))
                .collect(Collectors.joining(",", "[", "]"));
    }
}
