# Blue Button TLS Resources

This directory should contain a JKS keystore named bb.keystore that contains two entries:
- The client key and certificate used in mutual TLS authentication with the BlueButton server
- The BlueButton server's certificate, if self-signed

The keystore will primarily be used to by the BlueButtonClient to connect to the BlueButton sandbox backend during integration tests.

## Creating a Keystore from .pem

```bash
# convert client certificate/key pair to p12
openssl pkcs12 -export -in client-test-keypair.pem -out full-chain.keycert.p12 -name bb-dev-client -noiter -nomaciter

# import p12 to jks keystore (dest keystore doesn't necessarily have to exist)
keytool -importkeystore -srckeystore full-chain.keycert.p12 -destkeystore bb.keystore

# Add the server certificate, if self-signed
keytool -import -keystore bb.keystore -storepass changeit -alias bb-dev-selfsigned -file server.crt

```

## Useful Keytool CMDs

- List the certs in a keystore/truststore
```bash
keytool -list -keystore ./bb.keystore
```

- Delete a certificate from a keystore/truststore by alias
```bash
keytool -delete -keystore ./bb.keystore -alias your-alias-here
```

## External Resources

The current bbclient private key can be obtained from one of two places:

- https://confluence.cms.gov/pages/viewpage.action?spaceKey=BCDA&title=BB+Sandbox+Access (bundled, requires EUA)
- https://github.com/CMSgov/bcda-app/tree/main/shared_files (unbundled, public)
