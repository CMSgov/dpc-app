package gov.cms.dpc.macaroons;

import java.util.Optional;

class CaveatWrapper {

    private final CaveatVerifier verifier;

    CaveatWrapper(CaveatVerifier verifier) {
        this.verifier = verifier;
    }

    boolean verifyCaveat(String caveat) {
        final Optional<String> check = this.verifier.check(MacaroonCaveat.parseFromString(caveat));
        return check.isEmpty();
    }
}
