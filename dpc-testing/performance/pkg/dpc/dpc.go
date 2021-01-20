// Package dpc contains all test methods, the api test runner, and common functionalities
package dpc

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"strings"

	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
	luhn "github.com/joeljunstrom/go-luhn"
	regen "github.com/zach-klippenstein/goregen"
)

type Bundle struct {
	Total int              `json:"total"`
	Type  string           `json:"resourceType"`
	Entry []ResourceHolder `json:"entry"`
}

type ResourceHolder struct {
	Resource Resource `json:"resource"`
}

type Identifier struct {
	System string `json:system`
	Value  string `json:value`
}
type Resource struct {
	ID          string       `json:"id"`
	ClientToken []byte       `json:"token"`
	AccessToken string       `json:"access_token"`
	Type        string       `json:"resourceType"`
	Identifier  []Identifier `json:"identifier"`
}

// Pull `ids` out of a set of response bodies
func unmarshalIDs(resps [][]byte) []string {
	var IDs []string
	unmarshal(resps, func(result Resource) {
		IDs = append(IDs, result.ID)
	})
	return IDs
}

// Pull `identifier` out of a set of response bodies
func unmarshalIdentifiers(resps [][]byte, system string) []string {
	var identifierValue []string
	unmarshal(resps, func(result Resource) {
		for _, i := range result.Identifier {
			if i.System == system {
				identifierValue = append(identifierValue, i.Value)
			}
		}
	})
	return identifierValue
}

// Pull `clientTokens` out of a set of response bodies
func unmarshalClientTokens(resps [][]byte) [][]byte {
	var clientTokens [][]byte
	unmarshal(resps, func(result Resource) {
		clientTokens = append(clientTokens, result.ClientToken)
	})
	return clientTokens
}

// Pull `accessTokens` out of a set of response bodies
func unmarshalAccessTokens(resps [][]byte) []string {
	var accessTokens []string
	unmarshal(resps, func(result Resource) {
		accessTokens = append(accessTokens, result.AccessToken)
	})
	return accessTokens
}

func unmarshal(resps [][]byte, fn func(result Resource)) {
	for _, resp := range resps {
		var result Resource
		var err = json.Unmarshal(resp, &result)
		if err != nil {
			cleanAndPanic(err)
		}
		fn(result)
	}
}

func unmarshalBundle(resps [][]byte) []Bundle {
	var bundles []Bundle
	for _, resp := range resps {
		var result Bundle
		var err = json.Unmarshal(resp, &result)
		if err != nil {
			cleanAndPanic(err)
		}
		bundles = append(bundles, result)
	}
	return bundles
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

func generatePatientEntity(ids []string, file string) func() string {
	patients := make([]string, 0)
	for i := 0; i < len(ids); i++ {
		patient := string(templateBodyGenerator(file, map[string]func() string{"{patientID}": func() string { return ids[i] }})())
		patients = append(patients, patient)
	}
	return func() string {
		return strings.Join(patients[:], ",")
	}
}

func generateMBIFromFile(file string) func() string {
	body, err := ioutil.ReadFile(file)
	if err != nil {
		panic(err)
	}
	mbis := strings.Split(string(body), ",")
	return targeter.GenStrs(mbis)
}
