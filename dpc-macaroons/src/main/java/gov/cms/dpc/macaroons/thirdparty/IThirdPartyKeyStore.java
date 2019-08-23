package gov.cms.dpc.macaroons.thirdparty;

import java.util.Optional;

public interface IThirdPartyKeyStore {

    Optional<byte[]> getPublicKey(String location);

    void setPublicKey(String location, byte[] key);
}
