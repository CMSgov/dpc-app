#!/usr/bin/env python3
"""Standalone test script for CLEAR's Verification Sessions API.

Lets a developer quickly create, get, and search verification sessions
to inspect responses while testing. Independent from the OIDC login
script — this uses CLEAR_API_KEY, not the IDP client id/secret.
"""
from __future__ import annotations

import json
import os
from typing import Any
from urllib.parse import urlencode

import requests

from util import print_response

CLEAR_IDP_HOST = "verified.clearme.com"
CLEAR_API_BASE_URL = f"https://{CLEAR_IDP_HOST}/v1"
REQUEST_TIMEOUT_SECONDS = 30
SYNTHETIC_IDENTITY_EMAIL = "dogbeaker@aol.com"


def auth_headers(api_key: str, content_type: bool = False) -> dict[str, str]:
    headers = {"Authorization": f"Bearer {api_key}", "Accept": "application/json"}
    if content_type:
        headers["Content-Type"] = "application/json"
    return headers


def create_verification_session(
    api_key: str,
    project_id: str,
    email: str | None = None,
    phone: str | None = None,
    redirect_url: str | None = None,
    user_id: str | None = None,
    custom_fields: dict[str, Any] | None = None,
) -> requests.Response:
    body: dict[str, Any] = {"project_id": project_id}
    if email:
        body["email"] = email
    if phone:
        body["phone"] = phone
    if redirect_url:
        body["redirect_url"] = redirect_url
    if user_id:
        body["user_id"] = user_id
    if custom_fields:
        body["custom_fields"] = custom_fields

    return requests.post(
        f"{CLEAR_API_BASE_URL}/verification_sessions",
        json=body,
        headers=auth_headers(api_key, content_type=True),
        timeout=REQUEST_TIMEOUT_SECONDS,
    )


def fetch_verification_session(
    api_key: str,
    session_id: str,
    reveal_sensitive_data: bool = False,
) -> requests.Response:
    url = f"{CLEAR_API_BASE_URL}/verification_sessions/{session_id}"
    if reveal_sensitive_data:
        url = f"{url}?{urlencode({'reveal_sensitive_data': 'true'})}"
    return requests.get(url, headers=auth_headers(api_key), timeout=REQUEST_TIMEOUT_SECONDS)


def search_verification_sessions(
    api_key: str,
    email: list[str] | None = None,
    user_id: list[str] | None = None,
    status: list[str] | None = None,
    flow_id: list[str] | None = None,
    custom_fields: dict[str, str] | None = None,
    page_size: int = 25,
) -> requests.Response:
    params: dict[str, Any] = {"pageSize": page_size, "sortField": "updated_at", "sortDirection": "desc"}
    if email:
        params["email"] = email
    if user_id:
        params["userId"] = user_id
    if status:
        params["status"] = status
    if flow_id:
        params["flowId"] = flow_id
    if custom_fields:
        for key, value in custom_fields.items():
            params[f"custom_{key}"] = value

    return requests.get(
        f"{CLEAR_API_BASE_URL}/verification_sessions/search?{urlencode(params, doseq=True)}",
        headers=auth_headers(api_key),
        timeout=REQUEST_TIMEOUT_SECONDS,
    )


def main() -> int:
    api_key = os.environ["CLEAR_API_KEY"]
    project_id = os.environ["CLEAR_PROJECT_ID"]

    print("1) Create  2) Get  3) Search")
    choice = input("Choose an action: ").strip()

    if choice == "1":
        response = create_verification_session(api_key, project_id, email=SYNTHETIC_IDENTITY_EMAIL)
        print_response("create verification session", response)

    elif choice == "2":
        session_id = input("Verification session ID: ").strip()
        reveal = input("Reveal sensitive data? (y/N): ").strip().lower() == "y"
        response = fetch_verification_session(api_key, session_id, reveal_sensitive_data=reveal)
        print_response("get verification session", response)

    elif choice == "3":
        email = input("Search by email (optional, press enter to skip): ").strip()
        response = search_verification_sessions(api_key, email=[email] if email else None)
        print_response("search verification sessions", response)

    else:
        print("Invalid choice; exiting.")
        return 0

    return 0


if __name__ == "__main__":
    raise SystemExit(main())