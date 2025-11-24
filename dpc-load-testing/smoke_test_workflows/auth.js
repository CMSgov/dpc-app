/*global console*/ 
/* eslint no-console: "off" */

import { check } from 'k6';
import exec from 'k6/execution'
import {
  createClientToken,
  getOrganizationByAccessToken,
  createPublicKey,
  retrieveAccessToken,
  validateJwt
} from '../dpc-api-client.js';
import {
  exportPublicKey,
  generateKey,
  makeJwt,
  signatureSnippet
} from '../generate-jwt.js'


export async function checkAuthWorkflow(data) {
  const idx = exec.vu.idInInstance - 1;
  const orgId = data.orgIds[idx];
  if (!orgId) {
    exec.test.abort('error indexing VU ID against orgIds array');
  }

  const token = data.tokens[idx];

  const createTokenResponse = createClientToken(token, `New token ${idx}`);
  const checkCreateToken = check(
    createTokenResponse,
    {
      'create token success': res => res.status == 200 || res.status == 201,
      'token in response': res => res.json().token
    }
  );
  const clientToken = createTokenResponse.json().token;

  // Go ahead and try to create public key even if fail to create token
  // Abort on create token failure afterwards
  
  const keyPair = await generateKey();
  const privateKey = keyPair['privateKey'];
  const publicKey = await exportPublicKey(keyPair['publicKey']);
  const snippet = await signatureSnippet(privateKey);
  const createPublicKeyResponse = createPublicKey(token, `New+Key+${idx}`, publicKey, snippet);
  const checkCreatePublicKey = check(
    createPublicKeyResponse,
    {
      'create public key success': res => res.status == 200 || res.status == 201,
      'key id in response': res => res.json().id
    }
  );
  if (!checkCreatePublicKey) {
    console.error(createPublicKeyResponse.body);
    exec.test.abort('Failed to create public key');
  }

  // Checking here because create public key does not depend on this
  if (!checkCreateToken) {
    console.error(createTokenResponse.body);
    exec.test.abort('Failed to create token.');
  }

  const publicKeyId = createPublicKeyResponse.json().id
  const jwt = await makeJwt(clientToken, publicKeyId, privateKey);

  const validateJwtResponse = validateJwt(jwt);
  const checkValidateJwt = check(
    validateJwtResponse,
    {
      'validate jwt returns 200': res => res.status == 200,
    }
  );
  if (!checkValidateJwt) {
    console.error(validateJwtResponse.body);
    exec.test.abort('Failed to validate jwt');
  }

  const accessTokenResponse = retrieveAccessToken(jwt);
  const checkAccessToken = check(
    accessTokenResponse,
    {
      'access token returns 200': res => res.status == 200,
      'access token present': res => res.json().access_token,
    }
  );
  if (!checkAccessToken) {
    console.error(accessTokenResponse.body);
    exec.test.abort('Failed to retrieve access token');
  }
  const accessToken = accessTokenResponse.json().access_token

  const getOrgByAccessTokenResponse = getOrganizationByAccessToken(accessToken);
  const checkGetOrgByAccessToken = check(
    getOrgByAccessTokenResponse,
    {
      'get org by access token returns 200': res => res.status == 200,
      'get org by access token matches orgId': res => res.json().entry[0].resource.id == orgId,
    }
  )
  if (!checkGetOrgByAccessToken) {
    exec.test.abort('Failed to get organization by access token');
  }
}
