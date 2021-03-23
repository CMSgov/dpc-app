package v2

import "net/http"

// Service is an interface for testing to be able to mock the services
type Service interface {
	PostService
	Get(w http.ResponseWriter, r *http.Request)
	Delete(w http.ResponseWriter, r *http.Request)
	Put(w http.ResponseWriter, r *http.Request)
}

type PostService interface {
	Post(w http.ResponseWriter, r *http.Request)
}
