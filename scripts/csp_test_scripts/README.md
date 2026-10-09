# Purpose

These scripts were written to automate oauth2 flow and review responses to /userinfo calls while working with the CLEAR team to get necessary fields for Portal invitation flow. An additional script was added to explore and test CLEAR's Verification Sessions API.

# Instructions for call_clear_userinfo.py


### Set necessary environment variables
From dpc-app/ root
```
make secure-envs
source ops/config/decrypted/local.env
export CLEAR_IDP_CLIENT_ID CLEAR_IDP_CLIENT_SECRET
```

### Run it
```
cd scripts/csp_test_scripts
python call_clear_userinfo.py
```

This will print out a URL for you to copy/paste into your browser:
```
Step 1: Open this authorization URL in your browser:
...
```
- Then, copy/paste one of the phone numbers from CLEAR's list of [synthetic identities](https://docs.clearme.com/docs/synthetic-test-users).
- You can press the "skip" button to get through the rest of CLEAR's prompts
- When you are redirect back to DPC Portal, you should get a Rail Debug error page
- That's fine, the important thing is that the callback URL includes the `code` parameter.
- Your terminal should print out 
```
Step 2: Paste the returned code or full redirect URL:
```
- Copy/paste the URL from that Rails error page into the terminal.
- It will automatically pull `code` and print the response from `/userinfo`.

---

# Instructions for call_clear_verification_session_api.py


### Set necessary environment variables
Retrieve the CLEAR API key and project ID from sops and create the following environment variables
```
export CLEAR_API_KEY=
export CLEAR_PROJECT_ID=
```

### Run it
```
cd scripts/csp_test_scripts
python call_clear_verification_session_api.py
```

This will print out to the console a menu with three options:
```
1) Create  2) Get  3) Search
Choose an action:
```

Enter `1`, `2`, or `3` and press Enter to pick which endpoint to test. Each option is described below.

---

### Option 1: Create
Creates a new verification session using the configured `CLEAR_PROJECT_ID`, prefilled with a synthetic test email (`dogbeaker@aol.com`).

**What happens:**
- No additional prompts — the script immediately sends the request.

**What to expect:**
- This will make a `POST` request to `/verification_sessions`. This should return a `200` response with a full verification session object, including the verification session ID (starts with `verify_`). Copy this if you want to test Option 2 or 3 against this session afterward.


---

### Option 2: Get
Retrieves the full details of an existing verification session by ID.

**What happens:**
- Prompts for the verification session ID:
  ```
  Verification session ID:
  ```
  Paste in an `id` value from a previous Create or Search response.
- Prompts whether to reveal sensitive data:
  ```
  Reveal sensitive data? (y/N):
  ```
  Enter `y` only if you intentionally need to see SPII such as SSNs or government ID images in the response. Defaults to `N` (hidden) if left blank.

**What to expect:**
- This will make a `GET` request to `/verification_sessions/{id}`, with `?reveal_sensitive_data=true` appended if you answered `y`. This should return a `200` response with the session's current state. Unlike Create, this reflects whatever has actually happened in the session since it was created — `status`, `checks`, `traits`, and `custom_fields` will be populated once the user has gone through CLEAR's verification flow.


---

### Option 3: Search
Searches for verification sessions matching a given email address.

**What happens:**
- Prompts for an email to search by:
  ```
  Search by email (optional, press enter to skip):
  ```
  Press Enter with no input to run the search with no filters (returns the most recently updated sessions overall).

**What to expect:**
- This will make a `GET` request to `/verification_sessions/search`. This should return a `200` response containing a `verifications` array (empty if nothing matches) and pagination fields (`links`, `searchAfter`).
- **Note:** A session searched for immediately after creation may not show up right away. CLEAR's search index updates asynchronously, so a session that exists and is retrievable via Option 2 can still return empty from Option 3 for a short time afterward. 
