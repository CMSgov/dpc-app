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
	FilePattern string   // Optional file pattern location(s)
	IDs         []string // Optional list of ids to append url
}

type orgTargeter struct {
	reqs chan Req
	orgTargetConfig
}

type Req struct {
	id   *string
	body []byte
}

func NewOrgTargeter(config orgTargetConfig) *orgTargeter {
	targeter := &orgTargeter{
		reqs:            make(chan Req),
		orgTargetConfig: config,
	}

	targeter.genReqs()

	return targeter
}

func (o *orgTargeter) genReqs() {
	reqs := make([]Req, 0)

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
			reqs = append(reqs, Req{body: body})
		}
	}

	update := len(reqs) != 0

	for i := range o.IDs {
		if update {
			reqs[i].id = &o.IDs[i]
		} else {
			reqs = append(reqs, Req{id: &o.IDs[i]})
		}
	}

	go func() {
		for {
			for _, req := range reqs {
				o.reqs <- req
			}
		}
	}()
}

func (o *orgTargeter) buildTarget(t *vegeta.Target) error {
	t.Method = o.Method

	req := o.nextRequest()

	t.URL = o.FullURL(req.id)

	t.Header = map[string][]string{"Authorization": {fmt.Sprintf("Bearer %s", o.AccessToken)}}
	if o.Method != "DELETE" {
		t.Header.Add("Accept", "application/fhir+json")

		// if neither GET or DELETE, must be POST or PUT
		if o.Method != "GET" {
			t.Header.Add("Content-Type", "application/fhir+json")
			if req.body != nil {
				t.Body = req.body
			}
		}
	}

	return nil
}

func (o *orgTargeter) Name() string {
	var id *string
	if o.Method != "POST" {
		s := "{id}"
		id = &s
	}
	return fmt.Sprintf("%s %s", o.Method, o.FullURL(id))
}

func (o *orgTargeter) FullURL(id *string) string {
	url := fmt.Sprintf("%s/%s", o.BaseURL, o.Path)
	if id != nil {
		url = url + "/" + *id
	}
	return url
}

func (o *orgTargeter) nextRequest() Req {
	return <-o.reqs
}

func testOrganizationEndpoints(accessToken string) {
	const endpoint = "Organization"

	o := NewOrgTargeter(orgTargetConfig{
		Method:      "POST",
		BaseURL:     apiURL,
		Path:        endpoint + "/$submit",
		AccessToken: string(goldenMacaroon), // accessToken doesn't work
		FilePattern: "../../src/main/resources/organizations/organization-*.json",
	})
	resps := runTestWithTargeter(o.Name(), o.buildTarget, 1, 1)

	orgIDs := make([]string, 0)
	for _, resp := range resps {
		var result Resource
		json.Unmarshal(resp, &result)
		orgIDs = append(orgIDs, result.ID)
	}

	o = NewOrgTargeter(orgTargetConfig{
		Method:      "DELETE",
		BaseURL:     apiURL,
		Path:        endpoint,
		AccessToken: string(goldenMacaroon), // accessToken doesn't work
		IDs:         orgIDs,
	})
	runTestWithTargeter(o.Name(), o.buildTarget, 1, 1)
}
