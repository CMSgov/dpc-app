# Blue Button TLS Resources

Should contain a JKS keystore named bb.keystore that contains two entries:
- The client key and certificate used in mutual TLS authentication with the BlueButton server
- The BlueButton server's certificate, if self-signed

## Useful Keytool CMDs

- Add a new certificate (server.crt) to the truststore
```bash
keytool -import -keystore ./bb.truststore -storepass changeit -alias bb-dev-selfsigned -file server.crt
```

- Add a new client certificate/key to the keystore
```bash
keytool -importkeystore -srckeystore full-chain.keycert.p12 -destkeystore bb.keystore
```

- List the certs in a keystore/truststore
```bash
keytool -list -keystore ./bb.keystore
```

- Delete a certificate from a keystore/truststore by alias
```bash
keytool -delete -keystore ./bb.keystore -alias your-alias-here
```
