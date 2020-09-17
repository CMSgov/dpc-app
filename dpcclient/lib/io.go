package lib

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"net/url"
)

func ReadSmallFile(path string) ([]byte, error) {
	// These files should never be large enough to require buffering
	/* #nosec -- Potential file inclusion via variable */
	data, err := ioutil.ReadFile(path)
	if err != nil {
		return nil, err
	}
	return data, nil
}

func WriteSmallFile(path string, content []byte) error {
	// These files should never be large enough to require buffering
	/* #nosec -- Potential file inclusion via variable */
	if err := ioutil.WriteFile(path, content, 0644); err != nil {
		return err
	}
	return nil
}

func GetAccessToken(authTokenBytes []byte, baseURL string) (string, error) {
	data := url.Values{}
	data.Set("grant_type", "client_credentials")
	data.Set("scope", "system/*.*")
	data.Set("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
	data.Set("client_assertion", string(authTokenBytes))

	resp, err := http.PostForm(fmt.Sprintf("%s/Token/auth", baseURL), data)
	// make sure we close the body if necessary when there's an error
	if resp != nil {
		defer func() {
			err = resp.Body.Close()
			if err != nil {
				log.Fatalf("error closing request to %s: %s", resp.Request.URL, err)
			}
		}()
	}
	if err != nil {
		return "", err
	}

	if resp.StatusCode != 200 {
		msg, _ := ioutil.ReadAll(resp.Body)
		return "", fmt.Errorf("status %d, %s", resp.StatusCode, msg)
	}

	type TokenResponse struct {
		Scope       string `json:"scope"`
		AccessToken string `json:"access_token"`
		TokenType   string `json:"token_type"`
		ExpiresIn   int64  `json:"expires_in"`
	}
	var t TokenResponse
	if err = json.NewDecoder(resp.Body).Decode(&t); err != nil {
		panic(fmt.Sprintf("unexpected token response format: %s", err.Error()))
	}
	//fmt.Printf("TokenResponse: %+v", t)

	return t.AccessToken, nil
}
