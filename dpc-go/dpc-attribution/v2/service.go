package v2

import "net/http"

// Service is an interface for testing to be able to mock the services
type Service interface {
	Get(w http.ResponseWriter, r *http.Request)
	Save(w http.ResponseWriter, r *http.Request)
}
