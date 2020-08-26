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
_Alert: You MUST create different client tokens for every vendor that works with the API. A single client token should not be shared with multiple vendors._
