package gov.cms.dpc.macaroons;

/**
 * Interface for generating a {@link MacaroonCaveat} when required
 */
@FunctionalInterface
public interface CaveatSupplier {
    /**
     * Freshly generated {@link MacaroonCaveat}
     * @return - {@link MacaroonCaveat}
     */
    MacaroonCaveat get();
}
