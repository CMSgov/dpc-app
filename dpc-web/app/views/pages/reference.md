# Authorization
-------------

Welcome to Data at the Point of Care pilot api PROGRAM!

# 1. DPC Account

Any Fee-for-Service provider or Health IT vendor may request access to synthetic data by creating a DPC Account. Follow the steps below:

* Request access to the sandbox environment.
* CMS 1st email: Welcome, confirmation link.
* CMS 2nd email: DPC will assign you to your organization and notify you with next steps and a link to your DPC Account.
* Log in to your DPC account at https://dpc.cms.gov to manage your client tokens and public keys.

# 2. Client Tokens

Client tokens help monitor who is accessing your DPC account. A client token is required to create an access token, which is needed with every request made to the API. This ensures every interaction with the API can be traced back to the person who created the client token.

### Prerequisites:

* A registered DPC Account
* CMS email stating you have been assigned to an organization

### Create your first client token

_Alert: You MUST create different tokens for every vendor that works with the API. A single client token should not be shared with multiple vendors._

Your first client token must be created through your DPC account. After successfully accessing the API, you may choose to add client tokens through the API or continue using your DPC account.

1. **Log in to your DPC Account** and select + New Token.
2. **Add a Label:** Title your token with a recognizable name that includes the environment for which you are requesting access.
3. Click "Create Token" to generate your client token.

PLACEHOLDER FOR IMAGE

_Alert: This is the only time that this client token will be visible to you. Ensure that the value is recorded in a safe and durable location._

### Create multiple client tokens

You may create as many tokens as you like via your DPC Account using the instructions above. You may also create multiple client tokens at once by making a `POST` request to the `/Token` endpoint.

This endpoint accepts two (optional) query parameters:

PLACEHOLDER FOR IMAGE

### Request:

```
POST /api/v1/Token
```

### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Token?label={token label}&expiration={ISO formatted dateTime} \
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/json' \
-H 'Content-Type: application/json' \
-X POST
```

### Response:

The response from the API will include the client_token in the token field.

```
{
  "id": "3c308f6e-0223-42f8-80c2-cab242d68afc",
  "tokenType": "MACAROON",
  "label": "Token for organization 46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0.",
  "createdAt": "2019-11-04T11:49:55.126-05:00",
  "expiresAt": "2020-11-04T11:49:55.095-05:00",
  "token:": "{client_token}"
}
```

_Alert: This is the only time that this client token will be visible to you. Ensure that the value is recorded in a safe and durable location._

## List all client tokens

Having created multiple client tokens, you may want to list them to reference ID's, expiration dates, or delete specific client tokens from your DPC account.

All client tokens registered by your organization for a given environment can be listed by making a `GET` request to the `/Token` endpoint. This will return a list of token IDs with details on when they were created, when they expire, and the label associated with that token.

### Request:

```
GET /api/v1/Token
```

### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Token \
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/json' \
-H 'Content-Type: application/json' \
-X GET
```

### Response:

```
{
  "created_at": "2019-11-04T11:49:55.126-05:00",
  "count": 3,
  "entities": [
    {
      "id": "3c308f6e-0223-42f8-80c2-cab242d68afc",
      "tokenType": "MACAROON",
      "label": "Token for organization 46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0.",
      "createdAt": "2019-11-04T11:49:55.126-05:00",
      "expiresAt": "2020-11-04T11:49:55.095-05:00"
    },
    {
      "id": "eef87627-db4b-4c08-8a27-e88a8343099d",
      "tokenType": "MACAROON",
      "label": "Token for organization 46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0.",
      "createdAt": "2019-11-04T11:50:06.101-05:00",
      "expiresAt": "2020-11-04T11:50:06.096-05:00"
    },
    {
      "id": "ea314eaa-1cf5-4d01-9ea7-1646099ca9fd",
      "tokenType": "MACAROON",
      "label": "Token for organization 46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0.",
      "createdAt": "2019-11-04T11:50:06.685-05:00",
      "expiresAt": "2020-11-04T11:50:06.677-05:00"
    }
  ]
}
```

## Delete client tokens

You may want to delete a client token from your organization if a vendor or group or no longer exists or needs access to the API.

Client tokens can be removed by sending a `DELETE` request to the `/Token` endpoint using the unique ID of the client_token. Client_token IDs can be found either at the creation or as the result of listing_client_tokens.

### Request:

```
DELETE /api/v1/Token/{client_token id}
```

### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Token/{client_token id} \
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/json' \
-H 'Content-Type: application/json' \
-X DELETE
```

### Response:

```
200 - Token was removed
```

# 3. Public Keys

Public keys verify that client token requests are coming from an authorized application. This is by verifying that the private key used to sign your JSON Web Token (JWT) also matches a public previously uploaded ot DPC.

**ALL files in this section myst be stored in ONE folder**
(private.pem, public.pem, snippet.txt, snippet.txt.sig, signature.sig files)

### Prerequisites:

- A registered DPC Account
- CMS email stating you have been assigned to an organization

## Upload your first public key

**1. Generate a private key**
Use the command invocation:

```
openssl genrsa -out ./private.pem 4096
```

_Alert: The contents of your private key (private.pem file) should be treated as sensitive/secret information. Take care in the handling and storage of this information as it is essential to the security of your connection with DPC._

**2. Generate a public key**

Use the command invocation:

```
openssl rsa -in ./private.pem -outform PEM -pubout -out ./public.pem
```

**3. Paste the contents** of your public key (`public.pem` file) into the 'Public Key' field in your DPC Account. You must include the "BEGIN PUBLIC KEY" and "END PUBLIC KEY" tags before and afer your key.

PLACEHOLDER

**4. Add a Label:** Title your public key with a descriptive name that can be easily recognized for future purposes.

**5. Proceed** to create your public key signature.

### Create a public key signature

#### 1. Download the snippet.txt file to create a signature

#### 2. Create your public key signature

Use the command invocation:

```
openssl dgst -sign private.pem -sha256 -out snippet.txt.sig snippet.txt
```

#### 3. Verify your public key signature

Use the command invocation:

```
openssl base64 -in snippet.txt.sig -out signature.txt
```

**Response must yield Verified Ok.**

#### 4. Generate a _verified_ public key signature.

Use the command invocation:

```
openssl base64 -in snippet.txt.sig -out snippet.txt.sigb64
```

**5. Paste the contents** of your verified public key signature (signature.txt file) into the 'Public Key Signature' field in you DPC Account.

**6. Click Add Key** to upload your public key.

### List all public keys

If you have created multiple public keys, you may want to list them to reference ID's, check expiration dates, or delete specific public keys from your DPC account.

#### Request:

```
GET /api/v1/Key
```

#### cURL command:

```
curl -v http://localhost:3002/v1/Key \
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/json' \
-H 'Content-Type: application/json' \
-X GET
```

#### Response:

The response from the API will include the client_token in the token field.

```
{
  "created_at": "2019-11-04T13:16:29.008-05:00",
  "count": 1,
  "entities": [
    {
      "id": "b296f9d2-1aae-4c59-b6c7-c759b9db5226",
      "publicKey": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmyI+y8vAAFcV4deNdyKC\nH16ZPU7tgwnUzvtEYOp6s0DFjzgaqWmYZd/CNlb1psi+J0ChtcL9+Cx3v+HwDqVx\nToQrEqJ8hMavtXnxm2jPoRaxmbIGjHZ6jfyMot5+CdP8Vr5o9G2WIUgzjhFwMEXh\nlYg97uZadLLVKVXYTl4HtluVX5y7p1Wh4vkyJFBiqrX7qAJXvr6PK7OUeZDeVsse\nOMm33VwgbQSGRw7yWNOw+H/RbpGQkAUtHvGYvo/qLeb+iJsF2zBtjnkTmk5I8Vlo\n4xzbqaoqZqsHp4NgCw+bq0Y6AWLE2yUYi/DOatOdIBfLxlpf/FAY3f5FbNjISUuL\nmwIDAQAB\n-----END PUBLIC KEY-----\n",
      "createdAt": "2019-11-04T13:16:29.008-05:00",
      "label": "test-key"
    }
  ]
}
```

### List a specific public key

If you have created multiple keys, you may want to list them to reference ID's, expiration dates, or delete specific public keys from your DPC account.

Specific public keys can be listed by making a `GET` request to the `/Key` endpoint using the unique id of the public key.

#### Request:

```
GET /api/v1/Key/{public key id}
```

#### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Key/{public key id} \
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/json' \
-H 'Content-Type: application/json' \
-X GET
```

#### Response:

The response from the API will include the client_token in the token field.

```
{
  "id": "b296f9d2-1aae-4c59-b6c7-c759b9db5226",
  "publicKey": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmyI+y8vAAFcV4deNdyKC\nH16ZPU7tgwnUzvtEYOp6s0DFjzgaqWmYZd/CNlb1psi+J0ChtcL9+Cx3v+HwDqVx\nToQrEqJ8hMavtXnxm2jPoRaxmbIGjHZ6jfyMot5+CdP8Vr5o9G2WIUgzjhFwMEXh\nlYg97uZadLLVKVXYTl4HtluVX5y7p1Wh4vkyJFBiqrX7qAJXvr6PK7OUeZDeVsse\nOMm33VwgbQSGRw7yWNOw+H/RbpGQkAUtHvGYvo/qLeb+iJsF2zBtjnkTmk5I8Vlo\n4xzbqaoqZqsHp4NgCw+bq0Y6AWLE2yUYi/DOatOdIBfLxlpf/FAY3f5FbNjISUuL\nmwIDAQAB\n-----END PUBLIC KEY-----\n",
  "createdAt": "2019-11-04T13:16:29.008-05:00",
  "label": "test-key"
}
```

### Delete public keys

You may need to delete a public key from your organization if a user no longer needs access or otherwise needs to be removed from the system. 

Public keys can be removed by sending a `DELETE` request to the `/Key` endpoint using the unique ID of the public key, which is returned either at creation, or as the result of listing the public keys.

#### Request:

```
DELETE /api/v1/Key/{public key ID}
```

#### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Key/{public key id} \
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/json' \
-H 'Content-Type: application/json' \
-X DELETE
```

#### Response:

The response from the API will include the client_token in the token field.
```
200 - Key was removed
```

# JSON Web Tokens

A JSON Web Token (JWT) authenticates your organization with DPC. If you have not generated your client token and public/private key pair through the DPC Portal, please obtain the following prerequisites before proceeding.

**Prerequisities:**

- A registered client token
- Your private key
- Your public key ID

Once completed, please download the DPC Tool below to generate your JWT.

[Download JWT Tool](public/snippet.txt)

The following instructions are to be completed via the JWT Tool downloaded onto your personal machine. This tool will ensure the secure confidentiality of your private key information and enforce best practices when handling sensitive information in the generation of your JWT.

1. Please input your Private Key.
2. Please input your Client Token.
3. Please input your Public Key ID.

     a. This ID can be found under the “Public Keys” section in your DPC Portal.
     PLACEHOLDER

4. Click "Generate JWT".
5. Copy "Your JWT" to begin validation for DPC.

## Validate a JSON Web Token for DPC

The DPC API supports a /Token/validate endpoint, which allows you to submit your signed JWT for DPC validation. The response may return an error message with details as to which claims or values on the JWT are missing or incorrect.

_Alert: This method DOES NOT validate the JWT signature, public key or client tokens, it merely verifies the necessary elements are present in the JWT entity._

#### Request:

```
POST /api/v1/Token/validate
```

#### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Token/validate \
-H 'Accept: application/json' \
-H 'Content-Type: text/plain' \
-X POST \
-d "{Signed JWT}"
```

#### Response:

The response from the API will return with a HTTP 200 if the JWT is valid, otherwise an error message will be returned.

## Access/Bearer Token

Obtaining an access_token and setting it as your bearer_token are the final steps in connecting to the DPC API. **The access_token must be set as the bearer_token in EVERY API request and has a maximum expiration time of FIVE MINUTES.**

#### Prerequisities:
- A valid JSON Web Token (JWT)

### Obtain an access_token

In order to receive an access_token, the valid JWT must be submitted to the `/Token/auth` endpoint via a `POST` request. The `POST` request is encoded as an `application/x-www-form-url`.

**1. Set the JWT as the client_assertion** form parameter.

**2. Add the remaining fields below:**

PLACEHOLDER

The endpoint response will be a JSON object, which contains:

1. Your access_token
2. The lifetime of your token (in seconds)
3. Authorized system scopes

#### Request:

```
POST /api/v1/Token/auth
```

#### cURL command:

```
curl -v "https://sandbox.dpc.cms.gov/api/v1/Token/auth" \
-H 'Content-Type: application/x-www-form-urlencoded' \
-H 'Accept: application/json' \
-X POST
-d "grant_type=client_credentials&scope=system%2F*.*&client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer&client_assertion={self-signed JWT}"
```

#### Response:

The endpoint response is a JSON object which contains the access_token, the lifetime of the token (in seconds) and the authorized system scopes.

```
{
 "access_token": "{access_token value}",
 "token_type": "bearer",
 "expires_in": 300,
 "scope": "system/*.*"
}
```

### Obtain a bearer_token

To obtain your bearer_token, set your access_token returned in the previous step as your bearer_token. You will need to set the "{access_token value}" from the previous response as a header in most of your API calls preceded by the word Bearer and a space.

As access tokens expire, you will need to generate new tokens. You will not need to create new JWT’s to create a new access token, unless you are making a call with a different client token or public key.

### Sample Javascript Code to create a JWT and obtain an Access Token

```
const jsrsasign = require('jsrsasign')
const fetch = require('node-fetch')
const { URLSearchParams } = require('url')
var dt = new Date().getTime();
var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
   var r = (dt + Math.random()*16)%16 | 0;
   dt = Math.floor(dt/16);
   return (c=='x' ? r :(r&0x3|0x8)).toString(16);
});
var data = {
   "iss": "W3sidiI6Miwib....==",  //THESE VALUES COME FROM THE MACAROON (CLIENT TOKEN) YOU OBTAINED FROM DPC
   "sub": "W3sidiI6Miwib....==",  // THE SAME VALUE GOES IN BOTH "iss" and "sub" fields
   "aud": "https://sandbox.dpc.cms.gov/api/v1/Token/auth",
   "exp": Math.round(new Date().getTime() / 1000) + 300,
   "iat": Math.round(Date.now()),
   "jti": uuid,
};
var secret = "-----BEGIN RSA PRIVATE KEY-----\n" +
   "MIIJKAIBAAKCAgEAyw/is619pPp2jxQBYHBsF75XrGYh27X/UKzrKsBAWKb3ymC9\n" +    //THIS IS THE PRIVATE KEY THAT IS ASSOCIATED WITH THE PUBLIC KEY
   "................................................................\n" +    //YOU REGISTERED WITH DPC
   "................................................................\n" +
   "-----END RSA PRIVATE KEY-----\n"; //PRIVATE KEY
//var sHeader = JSON.stringify("de56ae6d-e42c-4738-81e6-c23009797cd1");
const header = {
   'alg': 'RS384',
   'kid': 'XXXXXXXXXXXXXXXXX',  //THIS IS THE KEY ID HAT IS ASSOCIATED WITH THE PUBLIC KEY
   //YOU REGISTERED WITH DPC
}
var sPayload = JSON.stringify(data);
var sJWT = jsrsasign.jws.JWS.sign("RS384", header, sPayload, secret);
fetch('https://sandbox.dpc.cms.gov/api/v1/Token/auth', {
   method: 'POST',
   header: 'ACCEPT: application/json',
   body: new URLSearchParams({
       scope: "system/*.*",
       grant_type: "client_credentials",
       client_assertion_type: "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
       client_assertion: sJWT
   })
}).then(response => {
   if (response.ok) {
       response.json().then(json => {
           console.log(json);
       });
   }
});
```

# Attestation & Attribution
-------------

Before accessing Patient data, DPC must establish that you have a valid Patient-Practitioner relationship with CMS Medicare and Medicaid Beneficiaries.  This process is referred to as Attestation/Attribution in the DPC API.

You will need to register Practitioners in your Organization, register Patients in your care, and attribute Patients to the Practitioners treating them. You must also keep these attributions up-to-date by submitting an attestation that  testifies these relationships are valid with each submission.

_Alert: The DPC sandbox environment does not contain any preloaded test data._

## Load Sample Data

The DPC team has created a collection of sample Practitioner, Patient, and Group resources which can be used to get started in the sandbox environment. These Resources can be found in our public GitHub repository as JSON files. More details included in this README file.

### Uploading Practitioners:

We have included 4 Practitioner Resources that represent fictitious Practitioners that you can add to your Organization.

### Uploading Patients:

The Blue Button team maintains a list of 101 Patients, along with their MBIs, that can be used for matching existing synthetic data in sandbox. More details and the corresponding data files can be found on Blue Button’s page under Sample Beneficiaries.

Users can provide their own sample FHIR resources that fulfill the required FHIR profiles to DPC, but will need to ensure that all Patient resources have a Medicare Beneficiary Identifier (MBI) that matches a record in the Beneficiary FHIR Data Server (BFD) .

### Find Organization ID

You will need your organization ID to create an Attribution Group for Attestation.To find your Organization ID, sign-in to your DPC Account and locate your Organization ID within your token.

PLACEHOLDER

The Organization endpoint supports a GET /Organization operation, which allows the user to retrieve their Organization ID.

#### Request:

```
GET /api/v1/Organization
```

#### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Organization
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/fhir+json' \
-H 'Content-Type: application/fhir+json' \
-X GET \
```

#### Response:

```
{
  "resourceType": "Organization",
  "id": "351fbb5f-f2f9-4094-bc6f-2b3600bb56e9",
  "identifier": [
    {
      "system": "http://hl7.org/fhir/sid/us-npi",
      "value": "3905293015"
    }
  ],
  "name": "Happy Healthcare",
  "address": [
    {
      "use": "work",
      "type": "postal",
      "line": [
        "1 Main Street"
      ],
      "city": "Baltimore",
      "state": "MD",
      "postalCode": "21224",
      "country": "US"
    }
  ],
  "endpoint": [
    {
      "reference": "Endpoint/ccf649dd-5258-4c97-a378-449693e73997"
    }
  ]
}
```

# 1. Practitioners

Every organization is required to keep a list of Practitioner Resources who are authorized to have access to DPC data. The DPC Team has included four Practitioner Resources that represent fictitious Practitioners that can be added to your Organization.

#### Prerequisites:
- A registered DPC Account
- Access to the API: active Bearer {access_token}
- Practioner information:
        - First and Last Name
        - Type 1 National Provider Identifier (NPI)

## Add a Practitioner

To register a Practitioner at your Organization, you must create a Practitioner resource as a JSON file in FHIR format. The JSON file must be included in the BODY of your request with no encoding (raw) when uploading  via a POST request to the /Practitioner endpoint.

To create the Practitioner Resource, the JSON file may include additional attributes detailed in the FHIR Implementation Guide within the DPC Practitioner Profile, but at a minimum must include the Practitioner’s:

- First and Last Name
- Type 1 National Provider Identifier (NPI)

#### Request:

```
POST /api/v1/Practitioner
```

#### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Practitioner
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/fhir+json' \
-H 'Content-Type: application/fhir+json' \
-X POST \
-d @practitioner.json
```

## Add Multiple Practitioners

The Practitioner endpoint supports a $submit operation, which allows you to upload a Bundle of resources for registration in a single batch operation.
 
Each individual Practitioner resource in your Bundle must satisfy the requirements on how to add a Practitioner resource, otherwise a 422-Unprocessed Entity error will be returned.

PLACEHOLDER	[ Download ] - Sample practitioner_bundle.json

#### Request:

```
POST /api/v1/Practitioner/$submit
```

#### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Practitioner/\$submit
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/fhir+json' \
-H 'Content-Type: application/fhir+json' \
-X POST \
-d @practitioner_bundle.json
```

## List all Practitioners

The Practitioner endpoint supports a GET /Practitioner operation, which allows you to retrieve a Bundle of Practitioner resources. You may need to retrieve a Practitioner’s NPI when you get to the Attribution section.

#### Request: 

```
GET /api/v1/Practitioner
```

#### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Practitioner
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/fhir+json' \
-H 'Content-Type: application/fhir+json' \
-X GET \
```

## List a specific Practitioner

The Practitioner endpoint also supports a GET /Practitioner operation where you can supply an NPI number and receive the Practitioner resource. You may use this to identify a Practitioners’ system ID based off of an NPI.

#### Request:

```
GET /api/v1/Practitioner?identifier={{Practitioner NPI}}
```

#### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Practitioner
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/fhir+json' \
-H 'Content-Type: application/fhir+json' \
-X GET \
```

#### Response: 

```
{
  "resourceType": "Bundle",
  "type": "searchset",
  "total": 1,
  "entry": [
    {
      "resource": {
        "resourceType": "Practitioner",
        "id": "8d80925a-027e-43dd-8aed-9a501cc4cd91",
        "meta": {
          "lastUpdated": "2020-06-10T18:43:14.150+00:00",
          "profile": [
            "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-practitioner"
          ]
        },
        "identifier": [
          {
            "system": "http://hl7.org/fhir/sid/us-npi",
            "value": "2323232225"
          }
        ],
        "name": [
          {
            "family": "Holguín308",
            "given": [
              "Alejandro916"
            ]
          }
        ]
      }
    }
  ]
}

```

# 2. Patients

Every organization is required to maintain a list of patients which represent the patient population currently being treated at your facilities. 

Since there is not any preloaded data in DPC’s sandbox, the Blue Button 2.0 team maintains a list of 101 Patients, along with their MBIs, that can be used for matching existing synthetic data (such as from an organization’s training EMR) with valid sandbox MBIs. More details and the corresponding data files can be found here.

#### Prerequisites:

- A registered DPC Account
- Access to the API: active Bearer {access_token}
- Patient information:
    - First and last name
    - Birth date in YY-MM-DD format
    - Medicare Beneficiary Identifier (MBI)
    - Managing Organization ID
    - System ID

## Add a Patient

To register a Patient at your Organization, you must create a Patient Resource as a JSON file in FHIR format. The JSON file must be in the BODY of your request with no encoding (raw) when uploading via a POST request to the /Patient endpoint.

To create the Patient Resource, the JSON file may include additional attributes detailed in the FHIR Implementation Guide within the DPC Practitioner Profile, but at a minimum must include the Patient’s:

- First and last name
- Birth date in YY-MM-DD
- Medicare Beneficiary Identifier (MBI)
    - identifier: 
    {system: 'https://bluebutton.cms.gov/resources/variables/bene_id',  value:  'Value of the MBI number'}

- Managing Organization ID:
    - "managingOrganization": {
    "reference": "Organization/{ID}",	

- System ID:
    - This can be found by listing all patients or finding a specific 
    patient by their MBI.
    "resource": {
    "resourceType": "Patient",
    "id": "728b270d-d7de-4143-82fe-d3ccd92cebe4"

#### Request:

```
POST /api/v1/Patient
```

#### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Patient
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/fhir+json' \
-H 'Content-Type: application/fhir+json' \
-X POST \
-d @patient.json
```

#### Response:

```
{
 "resourceType": "Parameters",
 "parameter": [
   {
     "name": "resource",
     "resource": {
       "resourceType": "Bundle",
       "id": "synthetic-roster-bundle",
       "type": "collection",
       "entry": [
         {
           "resource": {
             "resourceType": "Patient",
             "id": "728b270d-d7de-4143-82fe-d3ccd92cebe4",
             "meta": {
               "versionId": "MTU1NDgxMjczNTM5MjYwMDAwMA",
               "lastUpdated": "2019-04-09T12:25:35.392600+00:00",
               "profile": [
                 "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-patient"
               ]
             },
             "identifier": [
               {
                 "system": "https://bluebutton.cms.gov/resources/variables/bene_id",
                 "value": "1SQ3F00AA00"
               }
             ],
             "name": [
               {
                 "use": "official",
                 "family": "Prosacco716",
                 "given": [
                   "Jonathan639"
                 ],
                 "prefix": [
                   "Mr."
                 ]
               }
             ],
             "birthDate": "1943-06-08",
           }
         }
       ]
     }
   }
 ]
}
```

## Add Multiple Patients

The Patient endpoint supports a $submit operation, which allows you to upload a Bundle of resources for registration in a single batch operation.

Each Patient Resource in your Bundle may include additional attributes detailed in the FHIR Implementation Guide within the DPC Patient Profile, but at a minimum must satisfy the requirements on how to add a Patient Resource, otherwise a 422 - Unprocessable Entity error will be returned.

PLACEHOLDER [ Download ] - Sample patient_bundle.json

#### Request:

```
POST /api/v1/Patient/$submit
```

#### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Patient/\$submit
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/fhir+json' \
-H 'Content-Type: application/fhir+json' \
-X POST \
-d @patient_bundle.json
```

## List all Patients

The Patient endpoint supports a GET /Patient operation, which allows you to retrieve a Bundle of Patient Resources. You may need to retrieve the system ID of patients when you get to the Attribution section.

#### Requests:

```
GET /api/v1/Patient
```

#### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Patient
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/fhir+json' \
-H 'Content-Type: application/fhir+json' \
-X GET \
```

## List a specific Patient

The Patient endpoint also supports a GET /Patient operation where you can supply the Patient MBI and receive the Patient Resource. You may use this to identify a Patient’s system ID based off of an MBI.

#### Request:

```
GET /api/v1/Patient?identifier={{Patient MBI}}
```

#### cURL command:

```
curl -v https://sandbox.dpc.cms.gov/api/v1/Patient
-H 'Authorization: Bearer {access_token}' \
-H 'Accept: application/fhir+json' \
-H 'Content-Type: application/fhir+json' \
-X GET \
```

#### Response

```
{
  "resourceType": "Bundle",
  "type": "searchset",
  "total": 1,
  "entry": [
    {
      "resource": {
      "resourceType": "Patient",
      "id": "995a1c0f-b6bc-4d16-b6b0-b8a6597c6e1d",
        "meta": {
        "lastUpdated": "2020-06-12T15:39:42.834+00:00",
        "profile": [
          "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-patient"
          ]
        },
      "identifier": [
        {
          "system": "http://hl7.org/fhir/sid/us-mbi",
          "value": "5S41C00AA00"
        }
      ],
      "name": [
        {
          "family": "Wyman904",
          "given": [
            "Cruz300"
          ]
        }
      ],
      "gender": "male",
      "birthDate": "1956-02-08",
      "managingOrganization": {
          "reference":
          "Organization/351fbb5f-f2f9-4094-bc6f-2b3600bb56e9"
        }
      }
    }
  ]
}
```

# 3. Attestation

CMS requires Practitioners to attest that they have a treatment related purpose for adding a patient to their Group each time they make a Group addition. This is accomplished by submitting an attestation with every request. Attestations are posted as a [Provenance]() Resource via the X-Provenance header, as outlined in the FHIR specification.

#### Prerequisites:

- Access to the API: active Bearer `{access_token}`
- 