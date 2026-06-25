/*global console*/
/* eslint no-console: "off" */

import encoding from 'k6/encoding';
import http from 'k6/http';

export function getToken(oauthTokenUrl, clientId, clientSecret, scope) {

  const payload = {
    grant_type: 'client_credentials',
    scope: scope
  };

  let encodedCreds = 'Basic ' + encodeCreds(clientId, clientSecret);
  const params = {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': encodedCreds
    },
  };

  const response = http.post(oauthTokenUrl, payload, params);
  if (response.status !== 200) {
    console.error(`Failed to retrieve token: ${response.body}`);
    return null;
  }

  const responseBody = response.json();
  return responseBody.access_token;
}

export function encodeCreds(userName, password) {
  return encoding.b64encode(`${userName}:${password}`);
}