package v2

import "net/http"

// Controller is an interface to be able to mock the controllers
type Controller interface {
	ReadController
	CreateController
	DeleteController
	UpdateController
	ExportController
}

// ReadController is an interface for reading
type ReadController interface {
	Read(w http.ResponseWriter, r *http.Request)
}

// CreateController is an interface for creating
type CreateController interface {
	Create(w http.ResponseWriter, r *http.Request)
}

// DeleteController is an interface for deleting
type DeleteController interface {
	Delete(w http.ResponseWriter, r *http.Request)
}

// UpdateController is an interface for updating
type UpdateController interface {
	Update(w http.ResponseWriter, r *http.Request)
}

// ExportController is an interface for exporting
type ExportController interface {
	Export(w http.ResponseWriter, r *http.Request)
}

// JobController is an interface for job status
type JobController interface {
	Status(w http.ResponseWriter, r *http.Request)
}

// FileController is an interface for getting a file
type FileController interface {
	GetFile(w http.ResponseWriter, r *http.Request)
}

// AuthController is an interface for reading
type AuthController interface {
	CreateSystem(w http.ResponseWriter, r *http.Request)
	AddKey(w http.ResponseWriter, r *http.Request)
	DeleteKey(w http.ResponseWriter, r *http.Request)
}
