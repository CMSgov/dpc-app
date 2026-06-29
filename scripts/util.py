""" util methods for use with invoke_clear_api_manually.py """

from __future__ import annotations

import base64
import json
from typing import Any

import requests


def decode_jwt(value: str) -> dict[str, Any] | None:
    parts = value.split(".")
    if len(parts) != 3:
        return None
    try:
        payload = parts[1] + "=" * (-len(parts[1]) % 4)
        return json.loads(base64.urlsafe_b64decode(payload.encode("ascii")))
    except (ValueError, json.JSONDecodeError):
        return None


def parsed_body(response: requests.Response) -> Any:
    text = response.text.strip()
    if not text:
        return ""

    try:
        body: Any = response.json()
    except ValueError:
        body = json.loads(text) if text.startswith('"') and text.endswith('"') else text

    if isinstance(body, str):
        jwt_payload = decode_jwt(body)
        if jwt_payload is not None:
            return {"jwt": body, "payload": jwt_payload}

    return body


def print_response(label: str, response: requests.Response) -> None:
    print(f"\n== {label} ==")
    print(f"{response.request.method} {response.url}")
    print(f"Status: {response.status_code}")
    print("Headers:")
    print(json.dumps(dict(response.headers), indent=2, sort_keys=True))
    print("Body:")
    print(json.dumps(parsed_body(response), indent=2, sort_keys=True))
