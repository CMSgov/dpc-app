package gov.cms.dpc.macaroons.store;

public interface IRootKeyStore {

    IDKeyPair create();

    String get(String macaroonID);
}
