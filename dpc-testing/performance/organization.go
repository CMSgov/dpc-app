package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"path/filepath"

	vegeta "github.com/tsenart/vegeta/lib"
)

type orgTargetConfig struct {
	Method      string
	BaseURL     string
	Path        string
	AccessToken string
	FilePattern string // Optional file pattern location(s)
	ID          string // Optional id to append to url
}

type orgTargeter struct {
	bodies chan []byte
	orgTargetConfig
}

func NewOrgTargeter(config orgTargetConfig) *orgTargeter {
	targeter := &orgTargeter{
		bodies:          make(chan []byte),
		orgTargetConfig: config,
	}

	targeter.genReqs()

	return targeter
}

func (o *orgTargeter) genReqs() {
	bodies := make([][]byte, 0)

	if o.FilePattern != "" {
		fileNames, err := filepath.Glob(o.FilePattern)
		if err != nil {
			panic(err)
		}

		for _, f := range fileNames {
			body, err := ioutil.ReadFile(f)
			if err != nil {
				panic(err)
			}
			bodies = append(bodies, body)
		}
	}

	go func() {
		for {
			for _, body := range bodies {
				o.bodies <- body
			}
		}
	}()
}

func (o *orgTargeter) buildTarget(t *vegeta.Target) error {
	t.Method = o.Method

	var body []byte
	if o.FilePattern != "" {
		body = o.nextRequest()
	}

	t.URL = o.FullURL(o.ID)

	t.Header = map[string][]string{"Authorization": {fmt.Sprintf("Bearer %s", o.AccessToken)}}
	if o.Method != "DELETE" {
		t.Header.Add("Accept", "application/fhir+json")

		// if neither GET or DELETE, must be POST or PUT
		if o.Method != "GET" {
			t.Header.Add("Content-Type", "application/fhir+json")
			if body != nil {
				t.Body = body
			}
		}
	}

	return nil
}

func (o *orgTargeter) Name() string {
	var id string
	if o.Method != "POST" {
		id = "{id}"
	}
	return fmt.Sprintf("%s %s", o.Method, o.FullURL(id))
}

func (o *orgTargeter) FullURL(id string) string {
	url := fmt.Sprintf("%s/%s", o.BaseURL, o.Path)
	if id != "" {
		url = url + "/" + id
	}
	return url
}

func (o *orgTargeter) nextRequest() []byte {
	return <-o.bodies
}

func testOrganizationEndpoints() {
	const endpoint = "Organization"

	o := NewOrgTargeter(orgTargetConfig{
		Method:      "POST",
		BaseURL:     apiURL,
		Path:        endpoint + "/$submit",
		AccessToken: string(goldenMacaroon), // Users cannot create orgs so the golden macaroon is required
		FilePattern: "../../src/main/resources/organizations/base-organization.json",
	})
	resp := runTestWithTargeter(o.Name(), o.buildTarget, 1, 1)

	var resource Resource
	json.Unmarshal(resp[0], &resource)

	// Every org requires its own access token
	pubKeyStr, privateKey, signature := generateKeyPairAndSignature()
	keyID := uploadKey(pubKeyStr, signature, resource.ID)
	clientToken := getClientToken(resource.ID)
	accessToken := refreshAccessToken(privateKey, keyID, clientToken)

	o = NewOrgTargeter(orgTargetConfig{
		Method:      "GET",
		BaseURL:     apiURL,
		Path:        endpoint,
		AccessToken: accessToken, // Users cannot create orgs so the golden macaroon is required
		ID:          resource.ID,
	})
	runTestWithTargeter(o.Name(), o.buildTarget, 5, 2)

	o = NewOrgTargeter(orgTargetConfig{
		Method:      "PUT",
		BaseURL:     apiURL,
		Path:        endpoint,
		AccessToken: accessToken, // Users cannot create orgs so the golden macaroon is required
		FilePattern: "../../src/main/resources/organizations/organization-*.json",
		ID:          resource.ID,
	})
	runTestWithTargeter(o.Name(), o.buildTarget, 5, 2)

	o = NewOrgTargeter(orgTargetConfig{
		Method:      "DELETE",
		BaseURL:     apiURL,
		Path:        endpoint,
		AccessToken: string(goldenMacaroon),
		ID:          resource.ID,
	})
	runTestWithTargeter(o.Name(), o.buildTarget, 1, 1)
}
