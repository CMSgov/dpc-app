package gov.cms.dpc.macaroons;

import com.github.nitram509.jmacaroons.Macaroon;

/**
 * Function signature for handling the discharge process of the given {@link MacaroonCaveat}
 */
@FunctionalInterface
public interface MacaroonDischarger {
    /**
     * Perform discharge operation
     *
     * @param caveat          - {@link MacaroonCaveat} to discharge
     * @param encryptedCaveat - {@link Byte} encrypted caveat body
     * @return - Discharged {@link Macaroon}
     */
    Macaroon getDischarge(MacaroonCaveat caveat, byte[] encryptedCaveat);
}
