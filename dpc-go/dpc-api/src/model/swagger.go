package model

//ProxyPublicKeyRequest struct to hold data for public key request
type ProxyPublicKeyRequest struct {
	PublicKey string `json:"public_key"`
	Signature string `json:"signature"`
}
