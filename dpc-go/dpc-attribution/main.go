package main

import (
	"fmt"
	"net/http"
	"os"

	"github.com/CMSgov/dpc/attribution"
)

func main() {
	router := attribution.NewDPCAttributionRouter()

	port := os.Getenv("ATTRIBUTION_PORT")
	if port == "" {
		port = "3001"
	}
	http.ListenAndServe(fmt.Sprintf(":%s", port), router)
}
