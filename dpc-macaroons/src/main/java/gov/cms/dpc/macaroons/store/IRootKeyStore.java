package gov.cms.dpc.macaroons.store;

public interface IRootKeyStore {

    String create();

    String get(String macaroonID);
}
