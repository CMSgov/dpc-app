// Package dpc contains all test methods, the api test runner, and common functionalities
package dpc

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"

	"github.com/joeljunstrom/go-luhn"
	regen "github.com/zach-klippenstein/goregen"
)

type Identifier struct {
	System string `json:system`
	Value  string `json:value`
}
type Resource struct {
	ID          string `json:"id"`
	ClientToken []byte `json:"token"`
	AccessToken string `json:"access_token"`
	Type        string `json:"resourceType"`
	Identifier  []Identifier
}

// Pull `ids` out of a set of response bodies
func unmarshalIDs(resps [][]byte) []string {
	var IDs []string
	for _, resp := range resps {
		var result Resource
		var err = json.Unmarshal(resp, &result)
		if err != nil {
			cleanAndPanic(err)
		}
		IDs = append(IDs, result.ID)
	}

	return IDs
}

// Pull `identifier` out of a set of response bodies
func unmarshalIdentifiers(resps [][]byte, system string) []string {
	var identifierValue []string
	for _, resp := range resps {
		var result Resource
		var err = json.Unmarshal(resp, &result)
		if err != nil {
			cleanAndPanic(err)
		}
		for _, i := range result.Identifier {
			if i.System == system {
				identifierValue = append(identifierValue, i.Value)
			}
		}
	}

	return identifierValue
}

// Pull `clientTokens` out of a set of response bodies
func unmarshalClientTokens(resps [][]byte) [][]byte {
	var clientTokens [][]byte
	for _, resp := range resps {
		var result Resource
		var err = json.Unmarshal(resp, &result)
		if err != nil {
			cleanAndPanic(err)
		}
		clientTokens = append(clientTokens, result.ClientToken)
	}

	return clientTokens
}

// Pull `accessTokens` out of a set of response bodies
func unmarshalAccessTokens(resps [][]byte) []string {
	var accessTokens []string
	for _, resp := range resps {
		var result Resource
		var err = json.Unmarshal(resp, &result)
		if err != nil {
			cleanAndPanic(err)
		}
		accessTokens = append(accessTokens, result.AccessToken)
	}

	return accessTokens
}

const (
	FHIR  = "application/fhir+json"
	JSON  = "application/json"
	Plain = "text/plain"
	Form  = "application/x-www-form-urlencoded"
	Unset = ""
)

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

func generateNPI() string {
	luhnWithPrefix := luhn.GenerateWithPrefix(15, "808403")
	return luhnWithPrefix[len(luhnWithPrefix)-10:]
}

func generateMBI() string {
	mbi, err := regen.Generate("^[1-9][ac-hj-km-np-rt-yAC-HJ-KM-NP-RT-Y][ac-hj-km-np-rt-yAC-HJ-KM-NP-RT-Y0-9][0-9][ac-hj-km-np-rt-yAC-HJ-KM-NP-RT-Y][ac-hj-km-np-rt-yAC-HJ-KM-NP-RT-Y0-9][0-9][ac-hj-km-np-rt-yAC-HJ-KM-NP-RT-Y]{2}[0-9]{2}$")
	if err != nil {
		panic(err)
	}
	return mbi
}
