package gov.cms.dpc.macaroons;

import java.util.Optional;

/**
 * Wrapper class which translates {@link CaveatVerifier} into the format used by the underlying Macaroons library
 */
class CaveatWrapper {

    private final CaveatVerifier verifier;

    CaveatWrapper(CaveatVerifier verifier) {
        this.verifier = verifier;
    }

    boolean verifyCaveat(String caveat) {
        final Optional<String> check = this.verifier.check(MacaroonCaveat.parseFromString(caveat));
        // TODO: We need to improve the way we handle error messages. DPC-285
        return check.isEmpty();
    }
}
