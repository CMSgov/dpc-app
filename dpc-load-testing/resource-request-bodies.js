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
