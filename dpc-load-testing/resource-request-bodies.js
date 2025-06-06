export function generateOrganizationResourceBody (npi, name) {
	return {
		"resourceType": "Parameters",
		"parameter": [
			{
				"name": "resource",
				"resource": {
					"resourceType": "Bundle",
					"type": "collection",
					"entry": [
						{
							"resource": {
								"address": [
									{
										"use": "work",
										"type": "postal",
										"city": "New York",
										"country": "US",
										"line": [
											"101 1st Avenue",
											"Suite 1"
										],
										"postalCode": "11103",
										"state": "NY"
									}
								],
								"identifier": [
									{
										"system": "http://hl7.org/fhir/sid/us-npi",
										"value": npi
									}
								],
								"name": name,
								"resourceType": "Organization",
								"type": [
									{
										"coding": [
											{
												"code": "prov",
												"display": "Healthcare Provider",
												"system": "http://hl7.org/fhir/organization-type"
											}
										],
										"text": "Healthcare Provider"
									}
								]
							}
						}
					]
				}
			}
		]
	}
}

export function generateProviderResourceBody (npi) {
  return {
    "resourceType": "Practitioner",
    "active": true,
    "address": [
      {
        "city": "New York",
        "country": "US",
        "line": [
          "101 1st Avenue",
          "Suite 1"
        ],
        "postalCode": "11103",
        "state": "NY"
      }
    ],
    "gender": "female",
    "identifier": [
      {
        "system": "http://hl7.org/fhir/sid/us-npi",
        "value": npi
      }
    ],
    "meta": {
      "profile": [
        "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-practitioner"
      ],
      "lastUpdated": "2019-04-09T12:25:36.450182+00:00",
      "versionId": "MTU1NDgxMjczNjQ1MDE4MjAwMA"
    },
    "name": [
      {
        "family": "Klocko335",
        "given": [
          "Leonard963"
        ],
        "prefix": [
          "Dr."
        ]
      }
    ]
  }
}

export function generatePatientResourceBody(mbi) {
  return {
    "birthDate": "1972-11-11",
    "gender": "male",
    "name": [
      {
        "family": "Ruecker",
        "given": [
          "Kurt41"
        ]
      }
    ],
    "identifier": [
      {
        "system": "http://hl7.org/fhir/sid/us-mbi",
        "value": mbi
      }
    ],
    "resourceType": "Patient"
  }
}

export function generateGroupResourceBody(practitionerNpi, patientId) {
  const groupResource = 
  {
    "resourceType": "Group",
    "type": "person",
    "actual": true,
    "active": true,
    "characteristic": [
      {
        "code": {
          "coding": [
            {
                "code": "attributed-to"
            }
          ]
        },
        "valueCodeableConcept": {
          "coding": [
            {
                "system": "http://hl7.org/fhir/sid/us-npi",
                "code": practitionerNpi
            }
          ]
        }
      }
    ]
  }

  if (patientId != undefined) {
    groupResource["member"] = [
      {
        "entity": {
          "reference": "Patient/" + patientId
        }
      }
    ];
  };
  
  return groupResource;
}

export function generateProvenanceResourceBody(orgId, practitionerId) {
    return {
      "resourceType":"Provenance",
      "meta":
        {
        "profile": [
            "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-attestation"
        ]
        },
        "recorded":"2024-10-25T18:39:41.042Z",
        "reason": [
        {
            "system":"http://hl7.org/fhir/v3/ActReason",
            "code": "TREAT"
        }
        ],
        "agent": [
          {
            "role": [
              {
                "coding": [
                  {
                    "system": "http://hl7.org/fhir/v3/RoleClass",
                    "code": "AGNT"
                  }
                ]
              }
            ],
            "whoReference": 
            {
                "reference":"Organization/" + orgId
            },
            "onBehalfOfReference": 
            {
                "reference":"Practitioner/" + practitionerId
            }
          }
        ]
    }
}
