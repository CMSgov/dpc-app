package gov.cms.dpc.macaroons;

import com.github.nitram509.jmacaroons.Macaroon;

@FunctionalInterface
public interface MacaroonDischarger {
    Macaroon getDischarge(MacaroonCaveat caveat, byte[] encryptedCaveat);
}
