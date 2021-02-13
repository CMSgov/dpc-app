package main

import (
	"fmt"
	"github.com/CMSgov/dpc/attribution/repository"
	router2 "github.com/CMSgov/dpc/attribution/router"
	v2 "github.com/CMSgov/dpc/attribution/v2"
	"net/http"
	"os"
)

func main() {
	r := repository.NewOrganizationRepo(repository.GetDbConnection())
	c := v2.NewOrganizationService(r)

	defer r.Close()

	router := router2.NewDPCAttributionRouter(c)

	port := os.Getenv("ATTRIBUTION_PORT")
	if port == "" {
		port = "3001"
	}
	http.ListenAndServe(fmt.Sprintf(":%s", port), router)
}
