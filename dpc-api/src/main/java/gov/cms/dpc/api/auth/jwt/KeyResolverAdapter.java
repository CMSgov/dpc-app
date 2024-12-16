package gov.cms.dpc.api.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.LocatorAdapter;

import java.security.Key;

public abstract class KeyResolverAdapter extends LocatorAdapter<Key> {
    // Abstract class for LocatorAdapter<Key> after deprecation of SigningKeyResolverAdapter
    // See: https://javadoc.io/doc/io.jsonwebtoken/jjwt-api/0.12.2/io/jsonwebtoken/SigningKeyResolverAdapter.html
    public abstract Key resolveSigningKey(JwsHeader header, Claims claims);
}
