# Authorization
------------------

Welcome to Data at the Point of Care pilot API program!

## 1. DPC Account
Any Fee-for-Service provider or Health IT vendor may request access to the sandbox environment and synthetic data by creating an account in the DPC Portal. Follow the steps below:

1. [Request access to the sandbox environment.](https://dpc.cms.gov/users/sign_up)
2. CMS 1st email: Welcome, confirmation link.
3. CMS 2nd email: DPC will assign your account to an organization and notify you with the next steps and a link to the DPC Portal.
4. Log in to the DPC Portal at [https://dpc.cms.gov](https://dpc.cms.gov) to manage your client tokens and public keys.

## 2. Client Tokens
Client tokens help monitor who is accessing the API through your account. A client token is required to create an access token, which is needed with every request made to the API. This ensures every interaction with the API can be traced back to the person who created the client token.

### Prerequisites:
- A registered account in the DPC Portal
- CMS email stating you have been assigned to an organization

### Create your first client token
<div class="ds-c-alert ds-c-alert--warn">
  <div class="ds-c-alert__body">
    <p class="ds-c-alert__text">
      You MUST create different client tokens for every vendor that works with the API. A single client token should not be shared with multiple vendors.
    </p>
  </div>
</div>

Your first client token must be created through the DPC Portal. After successfully accessing the API, you may choose to add client tokens through the API or continue using the DPC Portal.

1. **Log in to your account in the DPC Portal** and select + New Token
2. **Add a Label:** Title your token with a recognizable name that includes the environment for which you are requesting access
3. Click "Create Token" to generate your client token

PLACEHOLDER FOR CLIENT TOKEN IMAGE

<div class="ds-c-alert ds-c-alert--warn">
  <div class="ds-c-alert__body">
    <p class="ds-c-alert__text">
      This is the only time that this client token will be visible to you. Ensure that the value is recorded in a safe and durable location.
    </p>
  </div>
</div>

### Create multiple client tokens

You may create as many tokens as you like via your account in the DPC Portal using the instructions above. You can also create multiple client tokens at once by making a POST request to the /Token endpoint.

This endpoint accepts two (optional) query parameters:

PLACEHOLDER FOR MULTIPLE TOKENS TABLE IMAGE

#### Request:

~~~sh
POST /api/v1/Token
~~~

#### cURL command:

<pre>
  <code>
curl -v https://sandbox.dpc.cms.gov/api/v1/Token?label={token label}&expiration={ISO formatted dateTime} \
    -H 'Authorization: Bearer {access_token}' \
    -H 'Accept: application/json' \
    -H 'Content-Type: application/json' \
    -X POST
  </code>
</pre>

#### Response:
The response from the API will include the client_token in the token field.

~~~json
{
  "id": "3c308f6e-0223-42f8-80c2-cab242d68afc",
  "tokenType": "MACAROON",
  "label": "Token for organization 46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0.",
  "createdAt": "2019-11-04T11:49:55.126-05:00",
  "expiresAt": "2020-11-04T11:49:55.095-05:00",
  "token:": "{client_token}"
}
~~~

<div class="ds-c-alert ds-c-alert--warn">
  <div class="ds-c-alert__body">
    <p class="ds-c-alert__text">
      This is the only time that this client token will be visible to you. Ensure that the value is recorded in a safe and durable location.
    </p>
  </div>
</div>