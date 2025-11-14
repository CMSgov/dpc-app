import { string2Uint8Array } from './generate-dpc-token.js';
import encoding from 'k6/encoding';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { urlRoot } from './dpc-api-client.js';

const algorithm = {
  name: 'RSASSA-PKCS1-v1_5',
  modulusLength: 4096,
  publicExponent: new Uint8Array([1, 0, 1]),
  hash: { name: 'SHA-256' }
};

export function jwtHeader(publicKeyId) {
  return {
    "alg": "RS256",
    "kid": publicKeyId,
    "typ": "JWT"
  }
}

export function jwtPayload(urlPrefix, clientToken, exp, jti) {
  return {
    "iss": clientToken,
    "sub": clientToken,
    "aud": urlPrefix + "/Token/auth",
    "exp": exp || Math.round(new Date().getTime()/1000) + 299,
    "jti": jti || uuidv4()
  }
}


export async function makeJwt(clientToken, publicKeyId, key) {
  
  const headerData = jwtHeader(publicKeyId);
  const payloadData = jwtPayload(urlRoot.replace('host.docker.internal', 'localhost'), clientToken);
  const jwt = await buildJwt(payloadData, key, headerData);
  return jwt;
}

export async function buildJwt(payloadData, key, headerData ) {
  const header = encoding.b64encode(JSON.stringify(headerData), "rawurl");
  const payload = encoding.b64encode(JSON.stringify(payloadData), "rawurl")
  const toSign = string2Uint8Array(header + "." + payload);
  const sig = await crypto.subtle.sign(algorithm, key, toSign);
  const signature = encoding.b64encode(sig, 'rawurl');
  return header + "." + payload + "." + signature;
}

export async function importKey(keyData){
  const importedKey = await crypto.subtle.importKey(
    'jwk',
    keyData,
    algorithm,
    true,
    ['sign', 'verify']
  );
  return importedKey;
}

export async function signatureSnippet(privateKey) {
  const snippet = string2Uint8Array("This is the snippet used to verify a key pair in DPC.")
  const signature = await crypto.subtle.sign(algorithm, privateKey, snippet);
  return encoding.b64encode(signature);
}
export async function generateKey() {
  const key = await crypto.subtle.generateKey(
    algorithm,
    true,
    ['sign', 'verify']
  );
  return key;
}
export async function exportPublicKey(publicKey) {
  const exportedPublicKey = await crypto.subtle.exportKey('spki', publicKey);
  const publicKeyString = encoding.b64encode(exportedPublicKey);
  return "-----BEGIN PUBLIC KEY-----\n" + publicKeyString + "\n-----END PUBLIC KEY-----"
}
