package main

import (
	"fmt"
	"net/http"
	"os"

	api "github.com/CMSgov/dpc/pkg/api"

	log "github.com/sirupsen/logrus"
)

func init() {
	log.SetFormatter(&log.JSONFormatter{})
	log.SetReportCaller(true)
}

func main() {
	router := api.NewDPCAPIRouter()

	port := os.Getenv("API_PORT")
	if port == "" {
		port = "3000"
	}
	http.ListenAndServe(fmt.Sprintf(":%s", port), router)
}
