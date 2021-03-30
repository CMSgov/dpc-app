package service

import "net/http"

// Service is an interface for testing to be able to mock the services
type Service interface {
	Get(w http.ResponseWriter, r *http.Request)
	Post(w http.ResponseWriter, r *http.Request)
	Delete(w http.ResponseWriter, r *http.Request)
	Put(w http.ResponseWriter, r *http.Request)
	Export(writer http.ResponseWriter, request *http.Request)
}
