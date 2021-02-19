package main

import (
	"fmt"
	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/CMSgov/dpc/attribution/router"
	v2 "github.com/CMSgov/dpc/attribution/v2"
	"log"
	"net/http"
	"os"
)

func main() {
	db := repository.GetDbConnection()
	defer func() {
		if err := db.Close(); err != nil {
			log.Fatal(err)
		}
	}()

	r := repository.NewOrganizationRepo(db)
	c := v2.NewOrganizationService(r)

	attributionRouter := router.NewDPCAttributionRouter(c)

	port := os.Getenv("ATTRIBUTION_PORT")
	if port == "" {
		port = "3001"
	}
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), attributionRouter); err != nil {
		log.Fatal(err)
	}
}
