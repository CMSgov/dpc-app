package v2

import "net/http"

type Controller interface {
	ReadController
	CreateController
}

type ReadController interface {
	Read(w http.ResponseWriter, r *http.Request)
}

type CreateController interface {
	Create(w http.ResponseWriter, r *http.Request)
}
