package dpc

import (
	"crypto/rsa"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"

	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

type Resource struct {
	ID          string `json:"id"`
	ClientToken []byte `json:"token"`
	AccessToken string `json:"access_token"`
}

// Pull `ids` out of a set of response bodies
func unmarshalIDs(resps [][]byte) []string {
	var IDs []string
	for _, resp := range resps {
		var result Resource
		json.Unmarshal(resp, &result)
		IDs = append(IDs, result.ID)
	}

	return IDs
}

// Pull `clientTokens` out of a set of response bodies
func unmarshalClientTokens(resps [][]byte) [][]byte {
	var clientTokens [][]byte
	for _, resp := range resps {
		var result Resource
		json.Unmarshal(resp, &result)
		clientTokens = append(clientTokens, result.ClientToken)
	}

	return clientTokens
}

// Pull `accessTokens` out of a set of response bodies
func unmarshalAccessTokens(resps [][]byte) []string {
	var accessTokens []string
	for _, resp := range resps {
		var result Resource
		json.Unmarshal(resp, &result)
		accessTokens = append(accessTokens, result.AccessToken)
	}

	return accessTokens
}

var FHIR = "application/fhir+json"
var JSON = "application/json"
var PLAIN = "text/plain"
var FORM = "application/x-www-form-urlencoded"
var UNSET = ""

func Headers(contentType, accept string) *targeter.Headers {
	return &targeter.Headers{
		ContentType: contentType,
		Accept:      accept,
	}
}

func CreateDirs() {
	err := os.MkdirAll("keys", os.ModePerm)
	if err != nil {
		cleanAndPanic(err)
	}

	err = os.MkdirAll("tokens", os.ModePerm)
	if err != nil {
		cleanAndPanic(err)
	}
}

func DeleteDirs() {
	err := os.RemoveAll("keys")
	if err != nil {
		fmt.Println("keys directory could not be deleted", err)
	}

	err = os.RemoveAll("tokens")
	if err != nil {
		fmt.Println("tokens directory could not be deleted", err)
	}
}

func cleanAndPanic(err error) {
	DeleteDirs()
	panic(err)
}

func readBodies(pattern string) [][]byte {
	filenames, err := filepath.Glob(pattern)
	if err != nil {
		panic(err)
	}

	bodies := make([][]byte, 0)
	for _, fname := range filenames {
		body, err := ioutil.ReadFile(fname)
		if err != nil {
			panic(err)
		}

		bodies = append(bodies, body)
	}

	return bodies
}

func generateKeyBodies(n int, fn func() (string, *rsa.PrivateKey, string)) [][]byte {
	var bodies [][]byte
	for i := 0; i < n; i++ {
		pubKeyStr, _, signature := fn()
		bodies = append(bodies, []byte(fmt.Sprintf("{ \"key\": \"%s\", \"signature\": \"%s\"}", pubKeyStr, signature)))
	}
	return bodies
}
