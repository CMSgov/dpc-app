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
