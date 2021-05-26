package service

import "net/http"

// Service is an interface for testing to be able to mock the services in the router test
type Service interface {
	Post(w http.ResponseWriter, r *http.Request)
	Get(w http.ResponseWriter, r *http.Request)
	Delete(w http.ResponseWriter, r *http.Request)
	Put(w http.ResponseWriter, r *http.Request)
	Export(w http.ResponseWriter, r *http.Request)
}
