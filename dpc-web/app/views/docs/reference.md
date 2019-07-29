## Information on accessing and working with the API
- [Join the Data at the Point of Care Google Group](https://groups.google.com/d/forum/dpc-api)


---

As patients move throughout the healthcare system, providers often struggle to gain and maintain a complete picture of their medical history.
The Data at the Point of Care (DPC) pilot project fills in the gaps with claims data to inform providers with structured patient history, past procedures, medication adherence, and more.

This API follows the workflow outlined by the FHIR Bulk Data Export specification, using the HL7 FHIR Standard. Claims data is provided as FHIR resources in NDJSON format.

This guide serves as a starting point for users to begin working with the API.

## Bulk Data

This project provides an implementation of the FHIR [Bulk Data Export](http://hl7.org/fhir/us/bulkdata/2019May/index.html) specification, which provides an async interface over the existing Blue Button 2.0 data model.
Details on the Blue Button data model can be found on its [project page](https://bluebutton.cms.gov).

This project will closely track changes in the underlying standard and is fully compliant with the current specification, with the following limitations:

- Type filters are not supported
- The `_since` parameter is not currently supported.
- Only `Group` level exporting is supported, not `Patient` or `System` level exports


In addition, the only available resource types are those exposed by Blue Button which include:

- Explanation of Benefits
- Patient
- Coverage

## Attribution

In order to receive data from the DPC application, a healthcare provider must have a treatment related purpose for viewing a patient's claims history.
Providers can attest to their treatment purposes by submitting a treatment roster which lists the patients currently under their care.

Given than existing standard for patient rosters does not exist, CMS is currently piloting an implementation of the [Attribution Guide](https://github.com/smart-on-fhir/smart-on-fhir.github.io/wiki/Bulk-data:-thoughts-on-attribution-lists-and-groups) currently under discussion with the SMART-ON-FHIR team.
The goal is to provide feedback to the group on experiences related to implementation and supporting the recommendations.

> Note: The attribution logic and interaction flow will be subject to revision over time.
CMS welcomes feedback on the implementation as well as experiences with other system.

Specific details on creating and updating treatment rosters is given in a later [section](#create-an-attribution-group).


Providers are required to keep their treatment rosters up to date, as patient attributions automatically expire after 90 days.
If an attribution expires, the provider may resubmit the patient to their roster and re-attest to a treatment purpose for another 90 days.

CMS currently restricts individual providers to no more than 5,000 attributed patients.
These restrictions are subject to change over time.

## Authentication and Authorization

The Data at the Point of Care pilot project is currently accessible as a private sandbox environment, which returns sample NDJSON files with synthetic beneficiary data.
There is no beneficiary PII or PHI in the files you can access via the sandbox.

The long-term goal of the project is to align with the security and authorization processes recommended by the [SMART Backend Services Guide](https://build.fhir.org/ig/HL7/bulk-data/authorization/index.html).
In the interim, access is controlled via API tokens issued by the DPC team.
All interactions with the server (except for requesting the Capability Statement) require that the token be present as a *Bearer* token in the `Authorization` request header.

~~~ sh
Authorization: Bearer {token}
~~~

**cURL command**

~~~
curl -H 'Authorization: Bearer {token}' {command to execute}
~~~

## Environment
The examples below include cURL commands, but may be followed using any tool that can make HTTP GET requests with headers, such as [Postman](https://getpostman.com).

### Examples

Examples are shown as requests to the DPC sandbox environment.
Be sure to include the CMS generated Access token in the requests, as documented in the [Authorization](#authentication-and-authorization) section.

## DPC Metadata

Metadata about the Data at the Point of Care (DPC) pilot project is available as a FHIR [CapabilityStatement](http://hl7.org/fhir/STU3/capabilitystatement.html) resource.

~~~ sh
GET /fhir/v1/metadata
~~~

**cURL command**

~~~sh
curl https://sandbox.dpc.cms.gov/fhir/v1/metadata
~~~


**Response**

~~~ json
{
  "resourceType": "CapabilityStatement",
  "description": "This Capability Statement defines the available resource, endpoints and operations supported by the Data @ the Point of Care Application.",
  "id": "dpc-capabilities",
  "version": "0.3.0-SNAPSHOT",
  "status": "draft",
  "date": "2019",
  "publisher": "Centers for Medicare and Medicaid Services",
  "kind": "capability",
  "instantiates": [
    "http://build.fhir.org/ig/HL7/bulk-data/CapabilityStatement-bulk-data"
  ],
  "software": {
    "name": "Data @ Point of Care API",
    "version": "0.3.0-SNAPSHOT",
    "releaseDate": "2019"
  },
  "fhirVersion": "3.0.1",
  "acceptUnknown": "extensions",
  "format": [
    "application/json",
    "application/fhir+json"
  ],
  "rest": [
    {
      "mode": "server",
      "resource": [
        {
          "type": "Endpoint",
          "profile": {
            "reference": "https://dpc.cms.gov/fhir/v1/StructureDefinition/dpc-profile-endpoint"
          },
          "interaction": [
            {
              "code": "read"
            },
            {
              "code": "search-type"
            }
          ],
          "versioning": "no-version"
        },
        {
          "type": "Organization",
          "profile": {
            "reference": "https://dpc.cms.gov/fhir/v1/StructureDefinition/dpc-profile-organization"
          },
          "interaction": [
            {
              "code": "read"
            }
          ],
          "versioning": "no-version"
        },
        {
          "type": "Patient",
          "profile": {
            "reference": "https://dpc.cms.gov/fhir/v1/StructureDefinition/dpc-profile-patient"
          },
          "interaction": [
            {
              "code": "read"
            },
            {
              "code": "create"
            },
            {
              "code": "update"
            },
            {
              "code": "delete"
            },
            {
              "code": "search-type"
            }
          ],
          "versioning": "no-version",
          "searchParam": [
            {
              "name": "identifier",
              "type": "string"
            }
          ]
        },
        {
          "type": "Practitioner",
          "profile": {
            "reference": "https://dpc.cms.gov/fhir/v1/StructureDefinition/dpc-profile-practitioner"
          },
          "interaction": [
            {
              "code": "read"
            },
            {
              "code": "create"
            },
            {
              "code": "update"
            },
            {
              "code": "delete"
            },
            {
              "code": "search-type"
            }
          ],
          "versioning": "no-version",
          "searchParam": [
            {
              "name": "identifier",
              "type": "string"
            }
          ]
        },
        {
          "type": "StructureDefinition",
          "interaction": [
            {
              "code": "read"
            },
            {
              "code": "search-type"
            }
          ],
          "versioning": "no-version"
        }
      ],
      "interaction": [
        {
          "code": "batch"
        }
      ],
      "operation": [
        {
          "name": "Group level data export",
          "definition": {
            "reference": "http://build.fhir.org/ig/HL7/bulk-data/OperationDefinition-group-export"
          }
        }
      ]
    }
  ]
}
~~~

## Exporting data

The primary interaction with the DPC pilot is via the FHIR `$export` operation.
This allows an organization to export data for their providers in an asynchronous and bulk manner.
Details on the operation itself can be found in the [FHIR Bulk Data Specification](https://build.fhir.org/ig/HL7/bulk-data/OperationDefinition-group-export.html).

Steps for creating and monitoring an export job are given in this section.


**1. Obtain an access token**

See the [Authentication and Authorization](#authentication-and-authorization) section above.

**2. Find the Provider Group**

Lookup the attribution [Group](https://hl7.org/fhir/STU3/group.html) resource associated to a specific provider using their National Provider Identity (NPI) number.
Creating attribution groups is covered later in this [reference](#attributing-patients-to-providers).

~~~ sh
GET /fhir/v1/Group?characteristic=attributed-to&characteristic-code={provider NPI}
~~~

**cURL command**

~~~ sh
curl -v https://sandbox.DPC.cms.gov/fhir/v1/Group?characteristic=attributed-to&characteristic-code=11349583 \
-H 'Authorization: Bearer {token}' \
-H 'Accept: application/fhir+json
~~~

This returns a [Bundle](https://hl7.org/fhir/STU3/bundle.html) resource which contains the attribution groups for the given provider.

**Response**

~~~ json
{
  "resourceType": "Bundle",
  "type": "searchset",
  "total": 1,
  "entry": [
    {
      "resource": {
        "resourceType": "Group",
        "id": "64d0cd85-7767-425a-a3b8-dcc9bdfd5402",
        "type": "person",
        "actual": true,
        "characteristic": {
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
                "value": "{provider NPI}"
              }
            ]
          }
        },
        "member": [
          {
            "entity": {
              "reference": "Patient/4d72ad76-fbc6-4525-be91-7f358f0fea9d"
            }
          },
          {
            "entity": {
              "reference": "Patient/74af8018-f3a1-469c-9bfa-1dfd8a646874"
            }
          }
        ]
      }
    }
  ]
}
~~~

Using the `Group.id` value of the returned resources a client can initiate an export job.   

**3. Initiate an export job**

~~~ sh
GET /fhir/v1/Group/{attribution group ID}/$export
~~~

To start an explanation of benefit data export job, a GET request is made to the ExplanationOfBenefit export endpoint.
An access token as well as Accept and Prefer headers are required.

The dollar sign (‘$’) before the word “export” in the URL indicates that the endpoint is an action rather than a resource. The format is defined by the FHIR Bulk Data Export spec.

**Headers**

- Authorization: Bearer {token}
- Accept: application/fhir+json
- Prefer: respond-async

**cURL command**

~~~ sh
curl -v https://sandbox.DPC.cms.gov/api/v1/Group/64d0cd85-7767-425a-a3b8-dcc9bdfd5402\$export \
-H 'Authorization: Bearer {token}' \
-H 'Accept: application/fhir+json' \
-H 'Prefer: respond-async'
~~~

**Response**

If the request was successful, a `202 Accepted` response code will be returned and the response will include a `Content-Location` header.
The value of this header indicates the location to check for job status and outcome.
In the example header below, the number 42 in the URL represents the ID of the export job.

**Headers**

- Content-Location: https://sandbox.dpc.cms.gov/fhir/v1/jobs/42


**4. Check the status of the export job**

> Note: The `Job` endpoint is not a FHIR resource and does not require the `Accept` header to be set to `application/fhir+json`.

**Request**

~~~ sh
GET https://sandbox.dpc.cms.gov/fhir/v1/jobs/42
~~~

Using the Content-Location header value from the data export response, you can check the status of the export job.
The status will change from 202 Accepted to 200 OK when the export job is complete and the data is ready to be downloaded.

**Headers**

- Authorization: Bearer {token}

**cURL Command**

~~~ sh
curl -v https://sandbox.dpc.cms.gov/fhir/v1/jobs/42 \
-H 'Authorization: Bearer {token}'
~~~

**Responses**

- `202 Accepted` indicates that the job is processing. Headers will include `X-Progress: In Progress`
- `200 OK` indicates that the job is complete. Below is an example of the format of the response body.

~~~ json
{
"transactionTime": "2018-10-19T14:47:33.975024Z",
"request": "https://sandbox.dpc.cms.gov/fhir/v1/Group/64d0cd85-7767-425a-a3b8-dcc9bdfd5402/$export",
"requiresAccessToken": true,
"output": [
  {
    "type": "ExplanationOfBenefit",
    "url": "https://sandbox.dpc.cms.gov/fhir/v1/data/42/DBBD1CE1-AE24-435C-807D-ED45953077D3.ndjson"
  }
],
"error": [
  {
    "type": "OperationOutcome",
    "url": "https://sandbox.dpc.cms.gov/fhir/v1/data/42/DBBD1CE1-AE24-435C-807D-ED45953077D3-error.ndjson"
  }
]
}
~~~

Claims data can be found at the URLs within the output field.
The number 42 in the data file URLs is the same job ID from the Content-Location header URL in previous step.
If some of the data cannot be exported due to errors, details of the errors can be found at the URLs in the error field.
The errors are provided in NDJSON files as FHIR [OperationOutcome](http://hl7.org/fhir/STU3/operationoutcome.html) resources.

**5. Retrieve the NDJSON output file(s)**

> Note: The `Data` endpoint is not a FHIR resource and doesn't require the `Accept` header to be set to `application/fhir+json`.

To obtain the exported explanation of benefit data, a GET request is made to the output URLs in the job status response when the job reaches the Completed state. The data will be presented as an NDJSON file of ExplanationOfBenefit resources.

**Request**

~~~ sh
GET https://sandbox.dpc.cms.gov/fhir/v1/data/42/DBBD1CE1-AE24-435C-807D-ED45953077D3.ndjson
~~~

**Headers**

- Authorization: Bearer {token}

**cURL command**

~~~sh
curl https://sandbox.dpc.cms.gov/fhir/v1/data/42/DBBD1CE1-AE24-435C-807D-ED45953077D3.ndjson \
-H 'Authorization: Bearer {token}'
~~~

**Response**

An example resource is shown below.

~~~ json
{
  "status":"active",
  "diagnosis":[
    {
      "diagnosisCodeableConcept":{
        "coding":[
          {
            "system":"http://hl7.org/fhir/sid/icd-9-cm",
            "code":"2113"
          }
        ]
      },
      "sequence":1,
      "type":[
        {
          "coding":[
            {
              "system":"https://bluebutton.cms.gov/resources/codesystem/diagnosis-type",
              "code":"principal",
              "display":"The single medical diagnosis that is most relevant to the patient's chief complaint or need for treatment."
            }
          ]
        }
      ]
    }
  ],
  "id":"carrier-10300336722",
  "payment":{
    "amount":{
      "system":"urn:iso:std:iso:4217",
      "code":"USD",
      "value":250.0
    }
  },
  "resourceType":"ExplanationOfBenefit",
  "billablePeriod":{
    "start":"2000-10-01",
    "end":"2000-10-01"
  },
  "extension":[
    {
      "valueMoney":{
        "system":"urn:iso:std:iso:4217",
        "code":"USD",
        "value":0.0
      },
      "url":"https://bluebutton.cms.gov/resources/variables/prpayamt"
    },
    {
      "valueIdentifier":{
        "system":"https://bluebutton.cms.gov/resources/variables/carr_num",
        "value":"99999"
      },
      "url":"https://bluebutton.cms.gov/resources/variables/carr_num"
    },
    {
      "valueCoding":{
        "system":"https://bluebutton.cms.gov/resources/variables/carr_clm_pmt_dnl_cd",
        "code":"1",
        "display":"Physician/supplier"
      },
      "url":"https://bluebutton.cms.gov/resources/variables/carr_clm_pmt_dnl_cd"
    }
  ],
  "type":{
    "coding":[
      {
        "system":"https://bluebutton.cms.gov/resources/variables/nch_clm_type_cd",
        "code":"71",
        "display":"Local carrier non-durable medical equipment, prosthetics, orthotics, and supplies (DMEPOS) claim"
      },
      {
        "system":"https://bluebutton.cms.gov/resources/codesystem/eob-type",
        "code":"CARRIER"
      },
      {
        "system":"http://hl7.org/fhir/ex-claimtype",
        "code":"professional",
        "display":"Professional"
      },
      {
        "system":"https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
        "code":"O",
        "display":"Part B physician/supplier claim record (processed by local carriers; can include DMEPOS services)"
      }
    ]
  },
  "patient":{
    "reference":"Patient/20000000000001"
  },
  "identifier":[
    {
      "system":"https://bluebutton.cms.gov/resources/variables/clm_id",
      "value":"10300336722"
    },
    {
      "system":"https://bluebutton.cms.gov/resources/identifier/claim-group",
      "value":"44077735787"
    }
  ],
  "insurance":{
    "coverage":{
      "reference":"Coverage/part-b-20000000000001"
    }
  },
  "item":[
    {
      "locationCodeableConcept":{
        "extension":[
          {
            "valueCoding":{
              "system":"https://bluebutton.cms.gov/resources/variables/prvdr_state_cd",
              "code":"99",
              "display":"With 000 county code is American Samoa; otherwise unknown"
            },
            "url":"https://bluebutton.cms.gov/resources/variables/prvdr_state_cd"
          },
          {
            "valueCoding":{
              "system":"https://bluebutton.cms.gov/resources/variables/prvdr_zip",
              "code":"999999999"
            },
            "url":"https://bluebutton.cms.gov/resources/variables/prvdr_zip"
          },
          {
            "valueCoding":{
              "system":"https://bluebutton.cms.gov/resources/variables/carr_line_prcng_lclty_cd",
              "code":"99"
            },
            "url":"https://bluebutton.cms.gov/resources/variables/carr_line_prcng_lclty_cd"
          }
        ],
        "coding":[
          {
            "system":"https://bluebutton.cms.gov/resources/variables/line_place_of_srvc_cd",
            "code":"99",
            "display":"Other Place of Service. Other place of service not identified above."
          }
        ]
      },
      "service":{
        "coding":[
          {
            "system":"https://bluebutton.cms.gov/resources/codesystem/hcpcs",
            "code":"45384",
            "version":"0"
          }
        ]
      },
      "extension":[
        {
          "valueCoding":{
            "system":"https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cd",
            "code":"3",
            "display":"Services"
          },
          "url":"https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cd"
        },
        {
          "valueQuantity":{
            "value":1
          },
          "url":"https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cnt"
        }
      ],
      "servicedPeriod":{
        "start":"2000-10-01",
        "end":"2000-10-01"
      },
      "quantity":{
        "value":1
      },
      "category":{
        "coding":[
          {
            "system":"https://bluebutton.cms.gov/resources/variables/line_cms_type_srvc_cd",
            "code":"2",
            "display":"Surgery"
          }
        ]
      },
      "sequence":1,
      "diagnosisLinkId":[
        2
      ],
      "adjudication":[
        {
          "category":{
            "coding":[
              {
                "system":"https://bluebutton.cms.gov/resources/codesystem/adjudication",
                "code":"https://bluebutton.cms.gov/resources/variables/carr_line_rdcd_pmt_phys_astn_c",
                "display":"Carrier Line Reduced Payment Physician Assistant Code"
              }
            ]
          },
          "reason":{
            "coding":[
              {
                "system":"https://bluebutton.cms.gov/resources/variables/carr_line_rdcd_pmt_phys_astn_c",
                "code":"0",
                "display":"N/A"
              }
            ]
          }
        },
        {
          "extension":[
            {
              "valueCoding":{
                "system":"https://bluebutton.cms.gov/resources/variables/line_pmt_80_100_cd",
                "code":"0",
                "display":"80%"
              },
              "url":"https://bluebutton.cms.gov/resources/variables/line_pmt_80_100_cd"
            }
          ],
          "amount":{
            "system":"urn:iso:std:iso:4217",
            "code":"USD",
            "value":250.0
          },
          "category":{
            "coding":[
              {
                "system":"https://bluebutton.cms.gov/resources/codesystem/adjudication",
                "code":"https://bluebutton.cms.gov/resources/variables/line_nch_pmt_amt",
                "display":"Line NCH Medicare Payment Amount"
              }
            ]
          }
        },
        {
          "category":{
            "coding":[
              {
                "system":"https://bluebutton.cms.gov/resources/codesystem/adjudication",
                "code":"https://bluebutton.cms.gov/resources/variables/line_bene_pmt_amt",
                "display":"Line Payment Amount to Beneficiary"
              }
            ]
          },
          "amount":{
            "system":"urn:iso:std:iso:4217",
            "code":"USD",
            "value":0.0
          }
        }
      ]
    }
  ]
}
~~~

## Attributing Patients to Providers

In order to export data from the DPC application, a healthcare provider must have attributed [Patient](https://hl7.org/fhir/STU3/patient.html) resources.
This attribution assertion attests to CMS that the provider has a treatment related purpose for accessing patient information.
More details on the attribution logic and rules are given [earlier](#attribution) in this reference.

### Create a Provider

An organization must first create a [Practitioner](http://hl7.org/fhir/STU3/practitioner.html) resource, which represents a healthcare provider that is associated with the organization.
This is accomplished by executing a `POST` request against the `Practitioner` resource, with the body containing a FHIR Practitioner resource.

~~~sh
POST /fhir/v1/Practitioner
~~~

Details on the exact data format are given in the [implementation guide]() but at a minimum, each resource must include:

- The NPI of the provider
- The provider's first and last name

**cURL command**

~~~sh
curl -v https://sandbox.dpc.cms.gov/fhir/v1/Practitioner
-H 'Authorization: Bearer {token}' \
-H 'Accept: application/fhir+json' \
-X POST \
-d @provider.json
~~~

**provider.json**

~~~json
{
  "resourceType": "Practitioner",
  "identifier": [
    {
      "system": "http://hl7.org/fhir/sid/us-npi",
      "value": "3116145044854423862"
    }
  ],
  "id": "0c527d2e-2e8a-4808-b11d-0fa06baf8254",
  "address": [
    {
      "city": "PLYMOUTH",
      "country": "US",
      "line": [
        "275 SANDWICH STREET"
      ],
      "postalCode": "02360",
      "state": "MA"
    }
  ],
  "gender": "male",
  "meta": {
    "lastUpdated": "2019-04-09T12:25:36.450182+00:00",
    "versionId": "MTU1NDgxMjczNjQ1MDE4MjAwMA"
  },
  "name": [
    {
      "family": "Klocko",
      "given": [
        "Leonard"
      ],
      "prefix": [
        "Dr."
      ]
    }
  ]
}
~~~

The `Practitioner.id` value of the returned resource can be used in the attribution group created in a later [section](#create-an-attribution-group).


### Create a Patient

The organization is also required to maintain a list of [Patient](http://hl7.org/fhir/STU3/patient.html) resources which represent the patient population currently being treated by their facilities.


~~~sh
POST /fhir/v1/Patient
~~~

Details on the exact data format are given in the [implementation guide]() but at a minimum, each resource must include:

- The MBI of the patient
- The patient's first and last name
- The patient's birthdate

**cURL command**

~~~sh
curl -v https://sandbox.dpc.cms.gov/fhir/v1/Patient
-H 'Authorization: Bearer {token}' \
-H 'Accept: application/fhir+json' \
-X POST \
-d @provider.json
~~~

**patient.json**

~~~json
{
  "resourceType": "Patient",
  "id": "728b270d-d7de-4143-82fe-d3ccd92cebe4",
  "meta": {
    "versionId": "MTU1NDgxMjczNTM5MjYwMDAwMA",
    "lastUpdated": "2019-04-09T12:25:35.392600+00:00"
  },
  "identifier": [
    {
      "system": "http://bluebutton.cms.hhs.gov/identifier#bene_id",
      "value": "20000000001809"
    }
  ],
  "name": [
    {
      "use": "official",
      "family": "Prosacco",
      "given": [
        "Jonathan"
      ],
      "prefix": [
        "Mr."
      ]
    }
  ],
  "telecom": [
    {
      "system": "phone",
      "value": "555-719-3748",
      "use": "home"
    }
  ],
  "gender": "male",
  "birthDate": "1943-06-08",
  "address": [
    {
      "extension": [
        {
          "url": "http://hl7.org/fhir/StructureDefinition/geolocation",
          "extension": [
            {
              "url": "latitude",
              "valueDecimal": 42.187011
            },
            {
              "url": "longitude",
              "valueDecimal": -71.30040
            }
          ]
        }
      ],
      "line": [
        "1038 Ratke Throughway Apt 10"
      ],
      "city": "Medfield",
      "state": "Massachusetts",
      "postalCode": "02052",
      "country": "US"
    }
  ],
  "maritalStatus": {
    "coding": [
      {
        "system": "http://hl7.org/fhir/v3/MaritalStatus",
        "code": "M",
        "display": "Married"
      }
    ],
    "text": "M"
  },
  "multipleBirthBoolean": false,
  "communication": [
    {
      "language": {
        "coding": [
          {
            "system": "urn:ietf:bcp:47",
            "code": "en-US",
            "display": "English (Region=United States)"
          }
        ],
        "text": "English"
      }
    }
  ]
}
~~~


The `Patient.id` value of the returned resource can be used in the attribution group created in a later [section](#create-an-attribution-group).

### Create an Attribution Group

Once the Provider and Patient records have been created, the final step is to associate the records into an attribution [Group](http://hl7.org/fhir/STU3/patient.html) resource, also known as a Patient roster.

Details on the exact data format are given in the [implementation guide]() but at a minimum, each resource must include:

- A list of `Patient` references
- The NPI of the provider which the patients are being attributed to

~~~sh
POST /fhir/v1/Group
~~~

**cURL command**

~~~sh
curl -v https://sandbox.dpc.cms.gov/fhir/v1/Group
-H 'Authorization: Bearer {token}' \
-H 'Accept: application/fhir+json' \
-X POST \
-d @group.json
~~~

**group.json**

~~~ json
"resource": {
        "resourceType": "Group",
        "type": "person",
        "actual": true,
        "characteristic": {
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
                "value": "110001029483"
              }
            ]
          }
        },
        "member": [
          {
            "entity": {
              "reference": "Patient/4d72ad76-fbc6-4525-be91-7f358f0fea9d"
            }
          },
          {
            "entity": {
              "reference": "Patient/74af8018-f3a1-469c-9bfa-1dfd8a646874"
            }
          }
        ]
      }
~~~

The `Group.id` value of the returned resource can be used by the client to initiate an [export job](#exporting-data).

### Update an Attribution Group

Patient attribution relationships automatically expire after 90 days and must be re-attested by the provider.
This is accomplished by resubmitting the patient to the provider's attribution Group.
This can be accomplished through the same endpoint described in the previous [section](#create-an-attribution-group).
Removing patients from the attribution Group is done by setting the `Group.member.inactive` value to `true` when resubmitting the Patient reference (as shown below).

Membership changes submitted to an existing attribution Group are always merged with the existing group state.
Consider the example Group resource shown below. From the previous example, we know that the provider with NPI *110001029483* has two attributed patients (*Patient/4d72ad76-fbc6-4525-be91-7f358f0fea9d* and *Patient/74af8018-f3a1-469c-9bfa-1dfd8a646874*).
By submitting a new roster with the information show below, the result with be *Patient/4d72ad76-fbc6-4525-be91-7f358f0fea9d* being removed from the roster and *Patient/bb151edf-a8b5-4f5c-9867-69794bcb48d1* being added.
The final state would be the provider having *Patient/74af8018-f3a1-469c-9bfa-1dfd8a646874* and *Patient/bb151edf-a8b5-4f5c-9867-69794bcb48d1* attributed.

***Add and remove attributed Patients***

~~~json
"resource": {
        "resourceType": "Group",
        "type": "person",
        "actual": true,
        "characteristic": {
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
                "value": "110001029483"
              }
            ]
          }
        },
        "member": [
          {
            "entity": {
              "reference": "Patient/4d72ad76-fbc6-4525-be91-7f358f0fea9d"
            },
            "inactive": true
          },
          {
            "entity": {
              "reference": "Patient/bb151edf-a8b5-4f5c-9867-69794bcb48d1"
            }
           }
        ]
      }
~~~