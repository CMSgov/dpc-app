package gov.cms.dpc.macaroons;

@FunctionalInterface
public interface CaveatSupplier {
    MacaroonCaveat get();
}
