package gov.cms.dpc.attribution.macaroons;

import gov.cms.dpc.macaroons.CaveatSupplier;
import gov.cms.dpc.macaroons.MacaroonCaveat;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class ExpirationCaveatSupplier implements CaveatSupplier {

    static final String EXPIRATION_KEY = "expires";

    ExpirationCaveatSupplier() {
        // Not used
    }

    @Override
    public MacaroonCaveat get() {
        final OffsetDateTime expiryTime = OffsetDateTime.now().plus(1, ChronoUnit.YEARS);
        return new MacaroonCaveat(EXPIRATION_KEY, MacaroonCaveat.Operator.GT, expiryTime.toString());
    }
}
