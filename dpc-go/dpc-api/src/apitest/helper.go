package apitest

import (
	"encoding/json"
	"github.com/CMSgov/dpc/api/model"
	"github.com/bxcodec/faker"
)

// Orgjson is a json string for testing purposes
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

const Practitionerjson = `{
  "resourceType": "Practitioner",
  "id": "f001",
  "text": {
    "status": "generated",
    "div": "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p><b>Generated Narrative with Details</b></p><p><b>id</b>: f001</p><p><b>identifier</b>: 938273695 (OFFICIAL), 129IDH4OP733 (USUAL)</p><p><b>name</b>: Eric van den broek (OFFICIAL)</p><p><b>telecom</b>: ph: 0205568263(WORK), E.M.vandenbroek@bmc.nl(WORK), fax: 0205664440(WORK)</p><p><b>address</b>: Galapagosweg 91 Den Burg 9105 PZ NLD (WORK)</p><p><b>gender</b>: male</p><p><b>birthDate</b>: 07/12/1975</p></div>"
  },
  "identifier": [
    {
      "use": "official",
      "system": "urn:oid:2.16.528.1.1007.3.1",
      "value": "938273695"
    },
    {
      "use": "usual",
      "system": "urn:oid:2.16.840.1.113883.2.4.6.3",
      "value": "129IDH4OP733"
    }
  ],
  "name": [
    {
      "use": "official",
      "family": "van den broek",
      "given": [
        "Eric"
      ],
      "suffix": [
        "MD"
      ]
    }
  ],
  "telecom": [
    {
      "system": "phone",
      "value": "0205568263",
      "use": "work"
    },
    {
      "system": "email",
      "value": "E.M.vandenbroek@bmc.nl",
      "use": "work"
    },
    {
      "system": "fax",
      "value": "0205664440",
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
    }
  ],
  "gender": "male",
  "birthDate": "1975-12-07"
}`

const Patientjson = `{
  "resourceType": "Patient",
  "id": "f001",
  "text": {
    "status": "generated",
    "div": "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p><b>Generated Narrative with Details</b></p><p><b>id</b>: f001</p><p><b>identifier</b>: 738472983 (USUAL), ?? (USUAL)</p><p><b>active</b>: true</p><p><b>name</b>: Pieter van de Heuvel </p><p><b>telecom</b>: ph: 0648352638(MOBILE), p.heuvel@gmail.com(HOME)</p><p><b>gender</b>: male</p><p><b>birthDate</b>: 17/11/1944</p><p><b>deceased</b>: false</p><p><b>address</b>: Van Egmondkade 23 Amsterdam 1024 RJ NLD (HOME)</p><p><b>maritalStatus</b>: Getrouwd <span>(Details : {http://terminology.hl7.org/CodeSystem/v3-MaritalStatus code 'M' = 'Married', given as 'Married'})</span></p><p><b>multipleBirth</b>: true</p><h3>Contacts</h3><table><tr><td>-</td><td><b>Relationship</b></td><td><b>Name</b></td><td><b>Telecom</b></td></tr><tr><td>*</td><td>Emergency Contact <span>(Details : {http://terminology.hl7.org/CodeSystem/v2-0131 code 'C' = 'Emergency Contact)</span></td><td>Sarah Abels </td><td>ph: 0690383372(MOBILE)</td></tr></table><h3>Communications</h3><table><tr><td>-</td><td><b>Language</b></td><td><b>Preferred</b></td></tr><tr><td>*</td><td>Nederlands <span>(Details : {urn:ietf:bcp:47 code 'nl' = 'Dutch', given as 'Dutch'})</span></td><td>true</td></tr></table><p><b>managingOrganization</b>: <a>Burgers University Medical Centre</a></p></div>"
  },
  "identifier": [
    {
      "use": "usual",
      "system": "urn:oid:2.16.840.1.113883.2.4.6.3",
      "value": "738472983"
    },
    {
      "use": "usual",
      "system": "urn:oid:2.16.840.1.113883.2.4.6.3"
    }
  ],
  "active": true,
  "name": [
    {
      "use": "usual",
      "family": "van de Heuvel",
      "given": [
        "Pieter"
      ],
      "suffix": [
        "MSc"
      ]
    }
  ],
  "telecom": [
    {
      "system": "phone",
      "value": "0648352638",
      "use": "mobile"
    },
    {
      "system": "email",
      "value": "p.heuvel@gmail.com",
      "use": "home"
    }
  ],
  "gender": "male",
  "birthDate": "1944-11-17",
  "deceasedBoolean": false,
  "address": [
    {
      "use": "home",
      "line": [
        "Van Egmondkade 23"
      ],
      "city": "Amsterdam",
      "postalCode": "1024 RJ",
      "country": "NLD"
    }
  ],
  "maritalStatus": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/v3-MaritalStatus",
        "code": "M",
        "display": "Married"
      }
    ],
    "text": "Getrouwd"
  },
  "multipleBirthBoolean": true,
  "contact": [
    {
      "relationship": [
        {
          "coding": [
            {
              "system": "http://terminology.hl7.org/CodeSystem/v2-0131",
              "code": "C"
            }
          ]
        }
      ],
      "name": {
        "use": "usual",
        "family": "Abels",
        "given": [
          "Sarah"
        ]
      },
      "telecom": [
        {
          "system": "phone",
          "value": "0690383372",
          "use": "mobile"
        }
      ]
    }
  ],
  "communication": [
    {
      "language": {
        "coding": [
          {
            "system": "urn:ietf:bcp:47",
            "code": "nl",
            "display": "Dutch"
          }
        ],
        "text": "Nederlands"
      },
      "preferred": true
    }
  ],
  "managingOrganization": {
    "reference": "Organization/f001",
    "display": "Burgers University Medical Centre"
  }
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
