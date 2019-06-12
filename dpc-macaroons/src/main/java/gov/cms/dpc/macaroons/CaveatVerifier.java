package gov.cms.dpc.macaroons;

import java.util.Optional;

@FunctionalInterface
public interface CaveatVerifier {

    Optional<String> check(MacaroonCaveat caveat);
}
