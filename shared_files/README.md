# Shared Files

What's in here and why?

* ssas
  * ATO_private.pem / ATO_public.pem: a private/public key pair used as a standin for an ACO's key pair for encryption testing. The private key does not have a passphrase. Do not reuse for other purposes.
  * admin_test_signing_key.pem / public_test_signing_key.pem: private keys that are used to sign tokens for the public and admin services. Do not reuse for purposes other than testing or local development.