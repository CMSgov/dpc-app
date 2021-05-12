package attributiontest

import (
	"encoding/json"

	"github.com/CMSgov/dpc/attribution/model/v2"

	"github.com/bxcodec/faker"
)

// Orgjson is a json string for testing purposes
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

// Implementerjson is an Implementer json string for testing purposes
const Implementerjson = `{
  "name": "test-Implementer"
}`

// JobJson is a Job json string for testing purposes
const JobJson = `{
  "id": "test-export-job"
}`

// OrgResponse provides a sample response that mimics what attribution service returns for testing purposes
func OrgResponse() *v2.Organization {
	o := v2.Organization{}
	_ = faker.FakeData(&o)
	var i v2.Info
	_ = json.Unmarshal([]byte(Orgjson), &i)
	o.Info = i
	return &o
}
