package main

import (
	"fmt"
	"github.com/CMSgov/dpc/api/client"
	v2 "github.com/CMSgov/dpc/api/v2"
	"net/http"
	"os"

	"github.com/CMSgov/dpc/api"
)

func main() {
	attributionURL, found := os.LookupEnv("ATTRIBUTION_URL")
	if !found {
		attributionURL = "http://localhost:3001"
	}

	c := v2.NewOrganizationController(&client.AttributionConfig{
		URL:     attributionURL,
		Retries: 3,
	})

	router := api.NewDPCAPIRouter(c)

	port := os.Getenv("API_PORT")
	if port == "" {
		port = "3000"
	}
	http.ListenAndServe(fmt.Sprintf(":%s", port), router)
}
