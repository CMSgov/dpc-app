# Authorization

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

### 1. Generate a private key
Use the command invocation:

```
openssl genrsa -out ./private.pem 4096
```

_Alert: The contents of your private key (private.pem file) should be treated as sensitive/secret information. Take care in the handling and storage of this information as it is essential to the security of your connection with DPC._


