# Authorization

Welcome to Data at the Point of Care pilot api PROGRAM!

## 1. DPC Account

Any Fee-for-Service provider or Health IT vendor may request access to synthetic data by creating a DPC Account. Follow the steps below:

* Request access to the sandbox environment.
* CMS 1st email: Welcome, confirmation link.
* CMS 2nd email: DPC will assign you to your organization and notify you with next steps and a link to your DPC Account.
* Log in to your DPC account at https://dpc.cms.gov to manage your client tokens and public keys.

## 2. Client Tokens

Client tokens help monitor who is accessing your DPC account. A client token is required to create an access token, which is needed with every request made to the API. This ensures every interaction with the API can be traced back to the person who created the client token.

*Prerequisites:*
* A registered DPC Account
* CMS email stating you have been assigned to an organization

#### Create your first client token

_Alert: You MUST create different tokens for every vendor that works with the API. A single client token should not be shared with multiple vendors._

Your first client token must be created through your DPC account. After successfully accessing the API, you may choose to add client tokens through the API or continue using your DPC account.

1. *Log in to your DPC Account* and select + New Token.
2. *Add a Label:* Title your token with a recognizable name that includes the environment for which you are requesting access.
3. *Select
