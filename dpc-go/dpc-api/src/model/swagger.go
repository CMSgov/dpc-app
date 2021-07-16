package model

type ProxyPublicKeyRequest struct {
	PublicKey string `json:"public_key"`
	Signature string `json:"signature"`
}
