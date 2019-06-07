package gov.cms.dpc.macaroons;

import com.github.nitram509.jmacaroons.Macaroon;
import com.github.nitram509.jmacaroons.MacaroonVersion;
import com.github.nitram509.jmacaroons.MacaroonsBuilder;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MacaroonsBakery {

    public static final Charset CAVEAT_CHARSET = Charset.forName("UTF-8");

    private final String location;

    public MacaroonsBakery(String location) {
        this.location = location;
    }

    public Macaroon createMacaroon(List<MacaroonCaveat> caveats) {
        final MacaroonsBuilder builder = new MacaroonsBuilder(location, "test-secret", "test-identifier");

        addCaveats(builder, caveats);

        return builder.getMacaroon();
    }

    public List<MacaroonCaveat> getCaveats(Macaroon macaroon) {
        return Arrays.stream(macaroon.caveatPackets)
                .map(MacaroonCaveat::parseFromPacket)
                .collect(Collectors.toList());
    }

    public byte[] serializeMacaroon(Macaroon macaroon) {
        return macaroon.serialize(MacaroonVersion.SerializationVersion.V2_JSON).getBytes(CAVEAT_CHARSET);
    }

    public Macaroon deserializeMacaroon(String serializedString) {
        return MacaroonsBuilder.deserialize(serializedString);
    }

    private void addCaveats(MacaroonsBuilder builder, List<MacaroonCaveat> caveats) {
        caveats
                .forEach(caveat -> {
                    // We'll need to expand this to support third-party caveats, at some point
                    if (caveat.isThirdParty()) {
                        throw new UnsupportedOperationException("We do not currently support third-party caveats");
                    }
                    builder.add_first_party_caveat(caveat.getCaveatText());
                });
    }
}
