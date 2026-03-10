import { b64encode } from 'k6/encoding';
import http from 'k6/http';

export function isArrayUnique(arr) {
  return Array.isArray(arr) && new Set(arr).size === arr.length;
}

export function isEmptyObject(obj) {
  return obj && Object.keys(obj).length === 0 && obj.constructor === Object;
}

export function isObjectType(obj, key) {
    return obj && obj[key] && typeof obj[key] === 'object' && !Array.isArray(obj[key]);
}

export function isArrayType(obj, key) {
    return obj && obj[key] && Array.isArray(obj[key]);
}

export function isDate(dateString) {
    const date = Date.parse(dateString);
    return !isNaN(date);
}

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
  return b64encode(`${userName}:${password}`);
}