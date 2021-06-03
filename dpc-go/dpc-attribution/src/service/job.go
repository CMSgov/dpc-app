package service

import (
	"net/http"
)

// JobService is an interface that defines what a JobService does
type JobService interface {
	Export(w http.ResponseWriter, r *http.Request)
	BatchesAndFiles(w http.ResponseWriter, r *http.Request)
}
