package lib

import (
	"crypto/rsa"
	"fmt"
	"os"
	"time"

	"github.com/gbrlsnchs/jwt/v3"
	"github.com/google/uuid"
)

func GenerateAuthToken(key *rsa.PrivateKey, kid string, macaroon []byte, baseURL string) ([]byte, error) {

	jti, err := uuid.NewRandom()
	if err != nil {
		fmt.Println(err)
		os.Exit(-1)
	}

	now := time.Now()
	pl := jwt.Payload{
		Issuer:         string(macaroon),
		Subject:        string(macaroon),
		Audience:       jwt.Audience{fmt.Sprintf("%s/Token/auth", baseURL)},
		ExpirationTime: jwt.NumericDate(now.Add(5 * time.Minute)),
		IssuedAt:       jwt.NumericDate(now),
		JWTID:          jti.String(),
	}

	alg := jwt.NewRS384(jwt.RSAPrivateKey(key))

	return jwt.Sign(pl, alg, jwt.KeyID(kid))
}
