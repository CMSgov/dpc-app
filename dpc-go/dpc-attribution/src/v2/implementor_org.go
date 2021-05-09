package v2

import (
    "bytes"
    "encoding/json"
    "fmt"
    "github.com/CMSgov/dpc/attribution/logger"
    "github.com/CMSgov/dpc/attribution/middleware"
    "github.com/CMSgov/dpc/attribution/model"
    "github.com/CMSgov/dpc/attribution/repository"
    "github.com/darahayes/go-boom"
    "go.uber.org/zap"
    "io/ioutil"
    "net/http"
    "strings"
    "text/template"
)

// ImplementerOrgService is a struct that defines what the service has
type ImplementerOrgService struct {
	implRepo   repository.ImplementerRepo
	orgRepo    repository.OrganizationRepo
	impOrgRepo repository.ImplementerOrgRepo
	autoCreateOrg bool
}

// NewImplementerOrgService function that creates an ImplementerOrg service and returns it's reference
func NewImplementerOrgService(implRepo repository.ImplementerRepo, orgRepo repository.OrganizationRepo, implOrgRepo repository.ImplementerOrgRepo, autoCreateOrg bool) *ImplementerOrgService {
	return &ImplementerOrgService{
		implRepo, orgRepo, implOrgRepo, autoCreateOrg,
	}
}

// Post function that saves the Implementer to the database and logs any errors before returning a generic error
func (ios *ImplementerOrgService) Post(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())

	implId := r.Context().Value(middleware.ContextKeyImplementer).(string)
	impl, err := ios.implRepo.FindByID(r.Context(), implId)
	if err != nil {
		log.Error("Failed to retrieve Implementer")
		boom.BadData(w, "Failed to retrieve Implementer")
		return
	}

	if impl == nil {
		log.Error("Implementer not found")
		boom.NotFound(w, "Implementer not found")
		return
	}

	body, _ := ioutil.ReadAll(r.Body)
	if len(body) == 0 {
		log.Error("Failed to create Implementer due to missing request body")
		boom.BadData(w, "Missing request body")
		return
	}

	var reqStruct = struct {
		Npi string `json:"npi"`
	}{}
	err = json.Unmarshal(body, &reqStruct)
	if err != nil {
		log.Error("Failed to parse body", zap.Error(err))
		boom.BadData(w, err)
		return
	}

	if reqStruct.Npi == "" {
		log.Error("missing npi", zap.Error(fmt.Errorf("missing npi in request body")))
		boom.BadData(w, err)
		return
	}


	org, _ := ios.orgRepo.FindByNPI(r.Context(), reqStruct.Npi)
	if org == nil {
	    if ios.autoCreateOrg {
            log.Error("Organization not found, creating new org")
            newOrg := buildFhirOrg(reqStruct.Npi, "New Organization")
            org, err = ios.orgRepo.Insert(r.Context(), []byte(newOrg))
            if err != nil {
                log.Error("Failed to create new org", zap.Error(err))
                boom.BadData(w, err)
                return
            }
        }else {
            log.Error("organization with provided NPI not found", zap.Error(err))
            boom.NotFound(w, err)
            return
        }
	} else{
        rel, err := ios.impOrgRepo.FindRelation(r.Context(), implId, org.ID)
        if err != nil {
            log.Error("unable to perform search for existing relation", zap.Error(err))
            boom.BadImplementation(w, err)
            return
        }

        //TODO determine if conflict error should be thrown, or re-activate
        if rel != nil {
            log.Error("relation already exists", zap.Error(err))
            boom.Conflict(w, "relation already exists")
            return
        }
    }

	ior, err := ios.impOrgRepo.Insert(r.Context(), implId, org.ID, model.Active)
	if err != nil {
		log.Error("Failed to create Implementer org relation", zap.Error(err))
		boom.BadData(w, err)
		return
	}

	iorBytes := new(bytes.Buffer)
	if err := json.NewEncoder(iorBytes).Encode(ior); err != nil {
		log.Error("Failed to convert orm model to bytes for Implementer org relation", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if _, err := w.Write(iorBytes.Bytes()); err != nil {
		log.Error("Failed to write Implementer org relation to response", zap.Error(err))
		boom.Internal(w, err.Error())
	}
}

func buildFhirOrg(npi string, name string) string {
	data := struct {
		Npi  string
		Name string
	}{
		npi,
		name,
	}

	orgTemplate := `{
	"resourceType": "Organization",
          "identifier": [
               {
                   "system": "http://hl7.org/fhir/sid/us-npi",
                   "value": "{{.Npi}}"
               }
          ],
          "name": "{{.Name}}"}`

	tmpl, err := template.New("FhirOrgTemplator").Parse(orgTemplate)
	if err != nil {
		panic(err)
	}

	b := new(strings.Builder)
	err = tmpl.Execute(b, data)
	if err != nil {
		panic(err)
	}
	return b.String()
}
