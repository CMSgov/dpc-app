package lib

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"errors"
	"fmt"
)

// https://dpc.cms.gov/docs, 'Uploading a public key' section. The first public key must be uploaded via the web UI.
// Thereafter, public keys can be managed via the API Key endpoint
const RsaKeyMinBits = 4096

// GenRSAKeyPair generates a suitably-sized private key for use with DPC, returning both the private key
// and the associated public key. Note that no pass phrase is associated with the private key. Further note
// that the private key is in PKCS1 form, but the public key is in PKIX form. This is directly in response
// to DPC requirements; DPC only understands PKIX format for public keys.
// https://serverfault.com/questions/9708/what-is-a-pem-file-and-how-does-it-differ-from-other-openssl-generated-key-file
func GenRSAKeyPair() (*rsa.PrivateKey, *rsa.PublicKey, error) {
	priv, err := rsa.GenerateKey(rand.Reader, RsaKeyMinBits)
	if err != nil {
		return nil, nil, err
	}
	return priv, &priv.PublicKey, nil
}

func savePrivateKey(prefix string, key *rsa.PrivateKey) error {
	convertedKey := x509.MarshalPKCS1PrivateKey(key)
	if convertedKey == nil {
		return errors.New("could not marshall private key to x509 PKCS1 form")
	}
	privateKeyBytes := pem.EncodeToMemory(
		&pem.Block{
			Type:  "RSA PRIVATE KEY",
			Bytes: convertedKey,
		},
	)
	if err := WriteSmallFile(fmt.Sprintf("%s-private.pem", prefix), privateKeyBytes); err != nil {
		return err
	}
	return nil
}

func savePublicKey(prefix string, key *rsa.PublicKey) error {
	convertedKey, err := x509.MarshalPKIXPublicKey(key)
	if convertedKey == nil || err != nil {
		return errors.New("could not marshall public key to x509 PKIX form")
	}
	publicKeyBytes := pem.EncodeToMemory(&pem.Block{
		Type:  "PUBLIC KEY",
		Bytes: convertedKey,
	})
	if err := WriteSmallFile(fmt.Sprintf("%s-public.pem", prefix), publicKeyBytes); err != nil {
		return err
	}
	return nil
}

// SaveDPCKeyPair saves an RSA Key Pair to files, first checking that it is valid for use with DPC.
// The prefix string should contain any path elements either relative to the working directory or absolute.
// Two files will be created using the prefix --- prefix-private.pem and prefix-public.pem ---
// with the obvious contents.
func SaveDPCKeyPair(prefix string, private *rsa.PrivateKey, public *rsa.PublicKey) error {
	if private == nil || public == nil {
		return errors.New("private and/or public key arguments cannot be nil")
	}
	if err := ValidateDPCKey(private); err != nil {
		return err
	}
	if err := savePrivateKey(prefix, private); err != nil {
		return err
	}
	if err := savePublicKey(prefix, public); err != nil {
		return err
	}
	return nil
}

// KeyFromPEM reads a PEM-formatted RSA private key and returns the rsa.PrivateKey and, for convenience, rsa.PublicKey.
func KeyFromPEM(bytes []byte) (*rsa.PrivateKey, *rsa.PublicKey, error) {
	if len(bytes) == 0 {
		return nil, nil, fmt.Errorf("empty or nil bytes")
	}

	block, rest := pem.Decode(bytes)
	if block == nil {
		return nil, nil, fmt.Errorf("unable to decode private key '%s'", string(rest))
	}
	key, err := x509.ParsePKCS1PrivateKey(block.Bytes)
	if err != nil {
		return nil, nil, fmt.Errorf("unable to parse RSA private key: %s", err.Error())
	}

	return key, &(key.PublicKey), nil
}

// ValidateDPCKey returns nil if the private key is a valid DPC PKI private key; otherwise, it returns an error
// Although DPC supports EC keys, this code does not (yet).
func ValidateDPCKey(key *rsa.PrivateKey) error {
	if RsaKeyMinBits > key.Size()*8 {
		return fmt.Errorf("key length (%d bits) less than minimum (%d bits)", key.Size()*8, RsaKeyMinBits)
	}
	return nil
}
