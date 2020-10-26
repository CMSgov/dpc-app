package targeter

import (
	"fmt"
	"os"
	"time"

	vegeta "github.com/tsenart/vegeta/lib"
)

type Config struct {
	Method      string // required
	BaseURL     string // required
	Endpoint    string // required
	AccessToken string // required

	ID     string   // optional id to use for GET/PUT/DELETE requests; Mutually exclusive with IDs
	IDs    []string // optional ids to use for GET/PUT/DELETE requests; Mutually exclusive with ID
	Body   []byte   // optional body for request; Mutually exclusive with Bodies
	Bodies [][]byte // optional bodies for requests; Mutually exclusive with Body

	Headers *Headers
}

type Targeter struct {
	nextBody func() []byte
	nextID   func() string
	Config
}

type Headers struct {
	ContentType, Accept string
}

func New(config Config) *Targeter {
	// Check for exclusivity for ids and bodies
	if config.ID != "" && config.IDs != nil {
		panic("Cannot set both `ID` and `IDs` for Targeter")
	}

	if config.Body != nil && config.Bodies != nil {
		panic("Cannot set both `Body` and `Bodies` for Targeter")
	}
	// If no Headers are passed, default to fhir json types
	if config.Headers == nil {
		config.Headers = &Headers{
			ContentType: "application/fhir+json",
			Accept:      "application/fhir+json",
		}
	}

	return &Targeter{
		nextBody: genBodies(config),
		nextID:   genIDs(config),
		Config:   config,
	}
}

func (dt *Targeter) Run(duration, frequency int) [][]byte {
	fmt.Printf("\nRunning performance test on %s...\n", dt.name())

	d := time.Second * time.Duration(duration)
	r := vegeta.Rate{Freq: frequency, Per: time.Second}

	attacker := vegeta.NewAttacker()
	var metrics vegeta.Metrics
	var respBodies [][]byte
	for results := range attacker.Attack(dt.buildTarget, r, d, fmt.Sprintf("%dps:", r.Freq)) {
		metrics.Add(results)
		respBodies = append(respBodies, results.Body)
	}
	metrics.Close()

	reporter := vegeta.NewTextReporter(&metrics)
	reporter.Report(os.Stdout)

	return respBodies
}

func (dt *Targeter) buildTarget(t *vegeta.Target) error {
	t.URL = dt.nextURL()
	t.Body = dt.nextBody()
	t.Method = dt.Method

	t.Header = map[string][]string{"Authorization": {fmt.Sprintf("Bearer %s", dt.AccessToken)}}
	if dt.Headers.ContentType != "" {
		t.Header.Add("Content-Type", dt.Headers.ContentType)
	}
	if dt.Headers.Accept != "" {
		t.Header.Add("Accept", dt.Headers.Accept)
	}

	return nil
}

func (dt *Targeter) nextURL() string {
	url := fmt.Sprintf("%s/%s", dt.BaseURL, dt.Endpoint)
	if dt.IDs != nil || dt.ID != "" {
		url = url + "/" + dt.nextID()
	}
	return url
}

// Name generates a human-readable name for a given targeter configuration
func (dt *Targeter) name() string {
	var id string
	if dt.IDs != nil {
		id = "{id}"
	}
	return fmt.Sprintf("%s %s/%s/%s", dt.Method, dt.BaseURL, dt.Endpoint, id)
}

// genIDs produces a closure that returns successive ids from the supplied list
func genIDs(config Config) func() string {
	// If `ID` is present it has precedence over `IDs`
	if config.ID != "" {
		return func() string { return config.ID }
	}

	i := 0
	n := len(config.IDs)
	return func() string {
		if i >= n {
			return ""
		}

		nextID := config.IDs[i]
		i++
		return nextID
	}
}

// genBodies produces a closure that returns successive request bodies from all files matching the pattern
func genBodies(config Config) func() []byte {
	// If `Body` is present it has precedence over `BodyFilePattern`
	if config.Body != nil {
		return func() []byte { return config.Body }
	}

	n := len(config.Bodies)
	i := 0
	return func() []byte {
		if i >= n {
			return nil
		}
		body := config.Bodies[i]
		i++
		return body
	}
}
