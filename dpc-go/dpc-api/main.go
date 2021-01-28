package main

import (
	"net/http"

	api "github.com/CMSgov/dpc/pkg/api"
)

func main() {
	router := api.NewDPCAPIRouter()
	http.ListenAndServe(":3000", router)
}
