package auth

import (
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/golang-jwt/jwt/v4"
	"github.com/pkg/errors"
)

type CommonClaims struct {
	ClientID string   `json:"cid,omitempty"`
	SystemID string   `json:"sys,omitempty"`
	Data     string   `json:"dat,omitempty"`
	Scopes   []string `json:"scp,omitempty"`
	ACOID    string   `json:"aco,omitempty"`
	UUID     string   `json:"id,omitempty"`
	jwt.StandardClaims
}

type Credentials struct {
	ClientID     string
	ClientSecret string
	ClientName   string
}

// SSASPlugin is an implementation of Provider that uses the SSAS API.
type SSASPlugin struct {
}

// MakeAccessToken mints an access token for the given credentials.
func (s SSASPlugin) MakeAccessToken(credentials Credentials) (string, error) {
	publicURL := "http://localhost:3103"
	url := fmt.Sprintf("%s/token", publicURL)

	req, err := http.NewRequest("POST", url, nil)
	if err != nil {
		return "", err
	}

	req.SetBasicAuth(credentials.ClientID, credentials.ClientSecret)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return "", errors.Wrap(err, "token request failed")
	}
	/* #nosec G307 */
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("token request failed; %v", resp.StatusCode)
	}

	type TokenResponse struct {
		AccessToken string `json:"access_token"`
		TokenType   string `json:"token_type"`
	}

	var t = TokenResponse{}
	if err = json.NewDecoder(resp.Body).Decode(&t); err != nil {
		return "", errors.Wrap(err, "could not decode token response")
	}

	ts := []byte(t.AccessToken)

	return string(ts), nil
}
