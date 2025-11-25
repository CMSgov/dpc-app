/*global console*/
/* eslint no-console: "off" */

import { check } from 'k6';
import { generateDPCToken } from '../generate-dpc-token.js';
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
  const idx = data.idx;
  const orgId = data.orgId;

  const token = generateDPCToken(orgId, data.goldenMacaroon);

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
  // Fail on create token failure afterwards

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
    console.error(`Failed to create public key for ${orgId}`);
    console.error(createPublicKeyResponse.body);
    return;
  }

  // Checking here because create public key does not depend on this
  if (!checkCreateToken) {
    console.error(`Failed to create client token for ${orgId}`);
    console.error(createTokenResponse.body);
    return;
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
    console.error(`Failed to validate jwt for ${orgId}`);
    console.error(validateJwtResponse.body);
    return;
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
    console.error(`Failed access token check for ${orgId}`);
    console.error(accessTokenResponse.body);
    return;
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
    console.error(`Failed get organization by access token for ${orgId}`);
    console.error(getOrgByAccessTokenResponse.body);
  }
}
