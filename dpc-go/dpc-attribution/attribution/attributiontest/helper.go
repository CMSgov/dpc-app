package attributiontest

import (
	"encoding/json"
	"github.com/CMSgov/dpc/attribution/model"
	"github.com/bxcodec/faker"
)

const Orgjson = `{
    "resourceType": "Organization",
    "identifier": [
      {
        "system": "http://hl7.org/fhir/sid/us-npi",
        "value": "2111111119"
      }
    ],
    "type": [
      {
        "coding": [
          {
            "system": "http://terminology.hl7.org/CodeSystem/organization-type",
            "code": "prov",
            "display": "Healthcare Provider"
          }
        ],
        "text": "Healthcare Provider"
      }
    ],
    "name": "BETH ISRAEL DEACONESS HOSPITAL - PLYMOUTH",
    "telecom": [
      {
        "system": "phone",
        "value": "5087462000"
      }
    ],
    "address": [
      {
        "use": "work",
        "type": "both",
        "line": [
          "275 SANDWICH STREET"
        ],
        "city": "PLYMOUTH",
        "state": "MA",
        "postalCode": "02360",
        "country": "US"
      }
    ]
  }`

func OrgResponse() *model.Organization {
	o := model.Organization{}
	_ = faker.FakeData(&o)
	var i model.Info
	_ = json.Unmarshal([]byte(Orgjson), &i)
	o.Info = i
	return &o
}
