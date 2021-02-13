package v2

import "net/http"

type Controller interface {
	ReadController
	CreateController
	DeleteController
	UpdateController
}

type ReadController interface {
	Read(w http.ResponseWriter, r *http.Request)
}

type CreateController interface {
	Create(w http.ResponseWriter, r *http.Request)
}

type DeleteController interface {
	Delete(w http.ResponseWriter, r *http.Request)
}

type UpdateController interface {
	Update(w http.ResponseWriter, r *http.Request)
}
