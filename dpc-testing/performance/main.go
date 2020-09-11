package main

import (
	"bytes"
	"flag"
	"fmt"
	"io/ioutil"
	"os"
	"time"

	vegeta "github.com/tsenart/vegeta/lib"
	"github.com/tsenart/vegeta/lib/plot"
)

var (
	baseURL, resultsPath, requestMethod, endpoint string
	freq, duration                                int
)

func init() {
	flag.StringVar(&baseURL, "base_url", "http://localhost:3002/v1", "Base URL of API")
	flag.StringVar(&resultsPath, "result_path", "results", "Path where results files will be written")
	flag.StringVar(&requestMethod, "request_method", "GET", "HTTP request method to test")
	flag.StringVar(&endpoint, "endpoint", "/metadata", "Endpoint to test")
	flag.IntVar(&duration, "duration", 5, "Total time to run the test, in seconds")
	flag.Parse()

	if _, err := os.Stat(resultsPath); os.IsNotExist(err) {
		err := os.MkdirAll(resultsPath, os.ModePerm)
		if err != nil {
			panic(err)
		}
	}
}

func main() {
	targeter := makeTarget()
	apiResults := runTest(targeter)

	var buf bytes.Buffer
	_, err := apiResults.WriteTo(&buf)
	if err != nil {
		panic(err)
	}

	//	writeResults(fmt.Sprintf("%s_%s_plot", requestMethod, strings.ReplaceAll(endpoint, "/", "")), buf)
}

func makeTarget() vegeta.Targeter {
	url := fmt.Sprintf("%s%s", baseURL, endpoint)

	accessToken := ""

	header := map[string][]string{
		"Prefer":        {"respond-async"},
		"Accept":        {"application/fhir+json"},
		"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
	}

	return vegeta.NewStaticTargeter(vegeta.Target{
		Method: "GET",
		URL:    url,
		Header: header,
	})
}

func runTest(target vegeta.Targeter) *plot.Plot {
	fmt.Printf("Running performance test on %s %s...\n", requestMethod, endpoint)
	title := plot.Title(fmt.Sprintf("test_%s_%s", requestMethod, endpoint))
	p := plot.New(title)
	defer p.Close()

	d := time.Second * time.Duration(duration)
	rate := vegeta.Rate{Freq: freq, Per: time.Second}
	plotAttack(p, target, rate, d)

	return p
}

func plotAttack(p *plot.Plot, t vegeta.Targeter, r vegeta.Rate, du time.Duration) {
	attacker := vegeta.NewAttacker()
	var metrics vegeta.Metrics
	for results := range attacker.Attack(t, r, du, fmt.Sprintf("%dps:", r.Freq)) {
		metrics.Add(results)
		err := p.Add(results)
		if err != nil {
			panic(err)
		}
	}
	metrics.Close()

	fmt.Printf("99th percentile: %s\n", metrics.Latencies.P99)
}

func writeResults(filename string, buf bytes.Buffer) {
	data := buf.Bytes()
	if len(data) > 0 {
		fn := fmt.Sprintf("%s/%s.html", resultsPath, filename)
		fmt.Printf("Writing results: %s\n", fn)
		err := ioutil.WriteFile(fn, data, 0600)
		if err != nil {
			panic(err)
		}
	}
}
