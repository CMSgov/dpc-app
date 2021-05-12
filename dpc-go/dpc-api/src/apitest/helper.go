package apitest

import (
	"encoding/json"

	"github.com/CMSgov/dpc/api/model"
	"github.com/bxcodec/faker"
)

// Orgjson is a organization json string for testing purposes
const Orgjson = `{
  "resourceType": "Organization",
  "identifier": [
    {
      "use": "official",
      "system": "urn:oid:2.16.528.1",
      "value": "91654"
    },
    {
      "use": "usual",
      "system": "urn:oid:2.16.840.1.113883.2.4.6.1",
      "value": "17-0112278"
    }
  ],
  "type": [
    {
      "coding": [
        {
          "system": "urn:oid:2.16.840.1.113883.2.4.15.1060",
          "code": "V6",
          "display": "University Medical Hospital"
        },
        {
          "system": "http://terminology.hl7.org/CodeSystem/organization-type",
          "code": "prov",
          "display": "Healthcare Provider"
        }
      ]
    }
  ],
  "name": "Burgers University Medical Center",
  "telecom": [
    {
      "system": "phone",
      "value": "022-655 2300",
      "use": "work"
    }
  ],
  "address": [
    {
      "use": "work",
      "line": [
        "Galapagosweg 91"
      ],
      "city": "Den Burg",
      "postalCode": "9105 PZ",
      "country": "NLD"
    },
    {
      "use": "work",
      "line": [
        "PO Box 2311"
      ],
      "city": "Den Burg",
      "postalCode": "9100 AA",
      "country": "NLD"
    }
  ],
  "contact": [
    {
      "purpose": {
        "coding": [
          {
            "system": "http://terminology.hl7.org/CodeSystem/contactentity-type",
            "code": "PRESS"
          }
        ]
      },
      "telecom": [
        {
          "system": "phone",
          "value": "022-655 2334"
        }
      ]
    },
    {
      "purpose": {
        "coding": [
          {
            "system": "http://terminology.hl7.org/CodeSystem/contactentity-type",
            "code": "PATINF"
          }
        ]
      },
      "telecom": [
        {
          "system": "phone",
          "value": "022-655 2335"
        }
      ]
    }
  ]
}`

// Groupjson is a group json string for testing purposes
const Groupjson = `
{
  "resourceType": "Group",
  "id": "fullexample",
  "meta": {
    "versionId": "1",
    "lastUpdated": "2019-06-06T03:04:12.348-04:00"
  },
  "extension": [
    {
      "url": "http://hl7.org/fhir/us/davinci-atr/StructureDefinition/ext-contractValidityPeriod",
      "valuePeriod": {
        "start": "2020-07-25",
        "end": "2021-06-24"
      }
    }
  ],
  "identifier": [
    {
      "use": "official",
      "type": {
        "coding": [
          {
            "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
            "code": "NPI",
            "display": "National Provider Identifier"
          }
        ]
      },
      "system": "https://sitenv.org",
      "value": "1316206220"
    },
    {
      "use": "official",
      "type": {
        "coding": [
          {
            "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
            "code": "TAX",
            "display": "Tax ID Number"
          }
        ]
      },
      "system": "https://sitenv.org",
      "value": "789456231"
    }
  ],
  "active": true,
  "type": "person",
  "actual": true,
  "name": "Test Group 3",
  "managingEntity": {
    "reference": "Organization/1",
    "display": "Healthcare related organization"
  },
  "member": [
    {
      "extension": [
        {
          "url": "http://hl7.org/fhir/us/davinci-atr/StructureDefinition/ext-changeType",
          "valueCode": "add"
        },
        {
          "url": "http://hl7.org/fhir/us/davinci-atr/StructureDefinition/ext-coverageReference",
          "valueReference": {
            "reference": "Coverage/1"
          }
        },
        {
          "url": "http://hl7.org/fhir/us/davinci-atr/StructureDefinition/ext-attributedProvider",
          "valueReference": {
            "type": "Practitioner",
            "identifier": {
                "system": "http://hl7.org/fhir/sid/us-npi",
                "value": "9941339108"
            }
          }
        }
      ],
      "entity": {
        "type": "Patient",
        "identifier": {
            "value": "2SW4N00AA00",
            "system": "http://hl7.org/fhir/sid/us-mbi"
        }
      },
      "period": {
        "start": "2014-10-08",
        "end": "2020-10-08"
      },
      "inactive": false
    }
  ]
}`

// FilteredGroupjson is a group json string for testing purposes
const FilteredGroupjson = `
{
  "resourceType": "Group",
  "type": "person",
  "actual": true,
  "name": "Test Group 3",
  "managingEntity": {
    "reference": "Organization/1",
    "display": "Healthcare related organization"
  },
  "member": [
    {
      "extension": [
        {
          "url": "http://hl7.org/fhir/us/davinci-atr/StructureDefinition/ext-attributedProvider",
          "valueReference": {
            "type": "Practitioner",
            "identifier": {
                "system": "http://hl7.org/fhir/sid/us-npi",
                "value": "9941339108"
            }
          }
        }
      ],
      "entity": {
        "type": "Patient",
        "identifier": {
            "value": "2SW4N00AA00",
            "system": "http://hl7.org/fhir/sid/us-mbi"
        }
      },
      "period": {
        "start": "2014-10-08",
        "end": "2020-10-08"
      },
      "inactive": false
    }
  ]
}`

// JobJSON is a Job json string for testing purposes
const JobJSON = `{
  "id": "test-export-job"
}`

// AttributionOrgResponse provides a sample organization response that mimics what attribution service returns for testing purposes
func AttributionOrgResponse() []byte {
	return AttributionResponse(Orgjson)
}

// AttributionResponse provides a sample response that mimics what attribution service returns for testing purposes
func AttributionResponse(fhir string) []byte {
	r := model.Resource{}
	_ = faker.FakeData(&r)
	r.ID = faker.ID

	var v map[string]interface{}
	_ = json.Unmarshal([]byte(fhir), &v)
	r.Info = v
	b, _ := json.Marshal(r)
	return b
}

// MalformedOrg provides a convenience method to get a non valid fhir resource, in this case an org
func MalformedOrg() []byte {
	var org map[string]interface{}
	_ = json.Unmarshal([]byte(Orgjson), &org)
	org["resourceType"] = "trash"
	b, _ := json.Marshal(org)
	return b
}
