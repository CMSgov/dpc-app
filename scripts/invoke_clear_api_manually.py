#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import secrets
import sys
from typing import Any
from urllib.parse import parse_qs, urlencode, urlparse

import requests

from util import decode_jwt, print_response


CLEAR_IDP_HOST = "verified.clearme.com"
REDIRECT_URI = "http://localhost:3100/auth/clear/callback"
REQUEST_TIMEOUT_SECONDS = 30
SYNTHETIC_IDENTITY_EMAIL = "dogbeaker@aol.com"

OIDC_CLAIMS = {
    "id_token": {
        "ssn9": None,
        "email": None,
        "email_verified": None,
        "given_name": None,
        "family_name": None,
    },
    "userinfo": {
        "ssn9": None,
        "email": None,
        "email_verified": None,
        "given_name": None,
        "family_name": None,
    },
}


def compact_json(value: dict[str, Any]) -> str:
    return json.dumps(value, separators=(",", ":"))


def clear_config() -> dict[str, str]:
    return {
        "host": CLEAR_IDP_HOST,
        "client_id": os.environ["CLEAR_IDP_CLIENT_ID"],
        "client_secret": os.environ["CLEAR_IDP_CLIENT_SECRET"],
        "redirect_uri": REDIRECT_URI,
        "state": secrets.token_hex(16),
        "nonce": secrets.token_hex(16),
    }


def auth_url(config: dict[str, str]) -> str:
    query = {
        "client_id": config["client_id"],
        "redirect_uri": config["redirect_uri"],
        "response_type": "code",
        "scope": "openid",
        "claims": compact_json(OIDC_CLAIMS),
        "nonce": config["nonce"],
        "state": config["state"],
    }
    return f"https://{config['host']}/integrations/oauth2/auth?{urlencode(query)}"


def auth_code(text: str) -> str:
    parsed = urlparse(text)
    query_code = parse_qs(parsed.query).get("code", [None])[0]
    fragment_code = parse_qs(parsed.fragment).get("code", [None])[0]
    return query_code or fragment_code or text


def token_url(config: dict[str, str]) -> str:
    return f"https://{config['host']}/integrations/oauth2/token"


def userinfo_url(config: dict[str, str], claims: dict[str, Any] | None) -> str:
    url = f"https://{config['host']}/integrations/userinfo"
    if claims is None:
        return url
    return f"{url}?{urlencode({'claims': compact_json(claims)})}"


def exchange_code(config: dict[str, str], code: str) -> requests.Response:
    return requests.post(
        token_url(config),
        data={
            "grant_type": "authorization_code",
            "code": code,
            "redirect_uri": config["redirect_uri"],
            "client_id": config["client_id"],
            "client_secret": config["client_secret"],
        },
        headers={"Accept": "application/json"},
        timeout=REQUEST_TIMEOUT_SECONDS,
    )


def fetch_userinfo(config: dict[str, str], access_token: str, claims: dict[str, Any] | None) -> requests.Response:
    return requests.get(
        userinfo_url(config, claims),
        headers={"Authorization": f"Bearer {access_token}", "Accept": "application/json, application/jwt"},
        timeout=REQUEST_TIMEOUT_SECONDS,
    )


def print_id_token(token_body: dict[str, Any]) -> None:
    id_token = token_body.get("id_token")
    if not id_token:
        print("\n== id_token payload ==\nNo id_token found in token response.")
        return

    print("\n== id_token payload ==")
    decoded = decode_jwt(id_token)
    if decoded is None:
        print("Unable to decode id_token.")
        return
    print(json.dumps(decoded, indent=2, sort_keys=True))


def main() -> int:
    config = clear_config()
    print("Step 1: Open this authorization URL in your browser:")
    print(auth_url(config))

    pasted_value = input("\nStep 2: Paste the returned code or full redirect URL: ").strip()
    if not pasted_value:
        print("No code entered; exiting.")
        return 0

    token_response = exchange_code(config, auth_code(pasted_value))
    print_response("token exchange", token_response)
    token_body = token_response.json()
    print_id_token(token_body)
    access_token = token_body["access_token"]
    print_response(label, fetch_userinfo(config, access_token, OIDC_CLAIMS))

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
