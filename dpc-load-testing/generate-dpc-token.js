import KJUR from 'https://unpkg.com/jsrsasign@11.1.0/lib/jsrsasign.js';
import { URLSearchParams } from 'https://jslib.k6.io/url/1.0.0/index.js';
import http from 'k6/http';
import encoding from 'k6/encoding';

const fetchTokenURL = 'https://test.dpc.cms.gov/api/v1/Token/auth';

const clientToken = __ENV.CLIENT_TOKEN;
const publicKeyId = __ENV.PUBLIC_KEY_ID;
let privateKey;

if (__ENV.ENVIRONMENT == 'local') {
  // RSA private keys are multi-line strings, and .env files aren't able to handle newline characters
  // for environment variables. As a workaround for local dev, we convert the private key into a base64 
  // encoded string, and set that to PRIVATE_KEY_BASE64 in .env
  const privateKeyBase64 = __ENV.PRIVATE_KEY_BASE64.trim();
  privateKey = encoding.b64decode(privateKeyBase64, 'std', 's');
}  else {
  privateKey = __ENV.PRIVATE_KEY;
}

function generateJWT() {
  let dt = new Date().getTime();
  const uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = (dt + Math.random()*16)%16 | 0;
    dt = Math.floor(dt/16);
    return (c=='x' ? r :(r&0x3|0x8)).toString(16);
  });

  const payload = {
      "iss": clientToken,
      "sub": clientToken, 
      "aud": fetchTokenURL,
      "exp": Math.round(new Date().getTime() / 1000) + 300,
      "iat": Math.round(Date.now()),
      "jti": uuid,
    };

  const header = {
    'alg': 'RS384',
    'kid': publicKeyId, 
  };

  return KJUR.jws.JWS.sign('RS384', header, JSON.stringify(payload), privateKey);
}

export default function generateDPCToken() {
  const signedJWT = generateJWT();
  const headers = { 
    'Accept': 'application/json',
    'Content-Type': 'application/x-www-form-urlencoded'
  };
  const body = new URLSearchParams({
    scope: 'system/*.*',
    grant_type: 'client_credentials',
    client_assertion_type: 'urn:ietf:params:oauth:client-assertion-type:jwt-bearer',
    client_assertion: signedJWT
  }).toString();
  return http.post(fetchTokenURL, body, { headers: headers })
}
