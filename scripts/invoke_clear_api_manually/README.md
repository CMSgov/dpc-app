# Instructions

### Set necessary environment variables
From dpc-app/ root
```
make secure-envs
source ops/config/decrypted/local.env
export CLEAR_IDP_CLIENT_ID CLEAR_IDP_CLIENT_SECRET
```

### Run it
```
cd scripts/invoke_clear_api_manually
python invoke_clear_api_manually.py
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
