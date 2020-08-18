const generateJWT = require('./jwt');
var jwt = require('jsonwebtoken');

beforeEach(() => {
  document.body.innerHTML = `
  <h1>Create a JWT</h1>
  <p>Environment:<br><input type="text" id="environment" style="width:100%;"></p>
  <p>Private Key:<br><textarea id="privateKey" rows="20" cols="100"></textarea></p>
  <p>Client Token:<br><textarea id="clientToken" rows="10" cols="100"></textarea></p>
  <p>Public Key ID:<br><input type="text" id="keyId" placeholder="xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" style="width:100%;"></p>
  <p><input type="button" id="JwtButton" value="Generate JWT"></p>
  <p>Unsigned JWT:<br><textarea id="unsignedJWT" rows="20" cols="100"></textarea></p>
  <p>Your JWT:<br><textarea id="JWT" rows="20" cols="100"></textarea></p>
  `;

  document.getElementById('JwtButton').addEventListener('click', generateJWT);
})


test('generates an unsigned JWT and JWT with user input', () => {  
  const clientToken = document.getElementById("clientToken");
  const env = document.getElementById("environment");
  const secret = document.getElementById("privateKey");
  const keyId = document.getElementById("keyId");
  
  const button = document.getElementById("JwtButton");
  const unsignedJwtOutput = document.getElementById("unsignedJWT");
  const jwtOutput = document.getElementById("JWT");

  clientToken.value = 'W3sidiI6MiwibCI6Imh0dHA6Ly9sb2NhbGhvc3Q6MzAwMiIsImkiOiIyZGI5ODhjMy0yNDAwLTQzNDktYmNhOS01ZDNkOWY0YjU5Y2YiLCJjIjpbeyJpNjQiOiJaSEJqWDIxaFkyRnliMjl1WDNabGNuTnBiMjRnUFNBeSJ9LHsiaTY0IjoiWlhod2FYSmxjeUE5SURJd01qRXRNRGd0TVRkVU1qRTZOVGM2TVRndU1UQTBOalkyV2cifSx7Imk2NCI6ImIzSm5ZVzVwZW1GMGFXOXVYMmxrSUQwZ056RTNabVZtT1RjdFptTmlOQzAwTW1Sa0xXSTVPRGN0WlRZelpqSTJOVGsxWmpjMCJ9LHsibCI6ImxvY2FsIiwiaTY0IjoiQW0wa1dzOXRKRnJQLUR1bW1Pd0FSbjMxcDZRTERsNEl2RUcxQlRQakVUbjlCeTBnZmk1SjN1ZzNfMy1nUUFwWjBrT1JhU0JyQ05uSGxqc253bW51dVlTYXRIQXpDRFZ0UWNvVkh4N0FjcTNmZTZDaXNONVFDNUVHQmlNaVp1dUZ5dFF5VGtSTDB3MXc3MWZCR0U2T2cxMnFXSXZZUWliZWx5U2E1ZElpako4cW1FRFJ2cDFlaGNYUk15MXF3eTBQSEl0S2FtOVlUUTJfR2tNUUFBIiwidjY0IjoiaEc1U2ExLWpIenAxdUFrMl8xamhWa1UxUGh1WmxlbzJneFB5OTlwcGpmdWZ3WU9xWlZfYnNUQ1VJLUZpdncydC0zNDJmR3pzR2xLTWRfT201OHRFSk52RGpoM2FNQ3ZxIn1dLCJzNjQiOiIzY2Q1X2ZNbkRfRWhWck1lcjFVUk9SQkRWVGJHZlZLMm04bGRmQ0hRVEw4In1d';
  env.value = 'prod-sbx';
  secret.value = `-----BEGIN RSA PRIVATE KEY-----
  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
  -----END RSA PRIVATE KEY-----`;
  keyId.value = 'a0991262-d368-4cd5-8695-350d6b94f970';

  button.click();

  unsignedJwt = JSON.parse(unsignedJwtOutput.value);
  jwtFill = jwtOutput.value;

  expect(unsignedJwt.aud).toMatch(env.value);
  expect(unsignedJwt.iss).toEqual(clientToken.value);
  expect(unsignedJwt.sub).toEqual(clientToken.value);
  expect(jwtFill).not.toBe('');
});