package main

import (
    "net/http"

	web "github.com/CMSgov/dpc/web"
)

func main() {
	router := web.NewDPCAPIRouter()
	http.ListenAndServe(":3000", router)
}
