package gov.cms.dpc.macaroons.thirdparty;

import java.security.PublicKey;
import java.util.Optional;

public interface IThirdPartyKeyStore {

    Optional<PublicKey> getPublicKey(String location);

    void setPublicKey(String location, PublicKey key);
}
