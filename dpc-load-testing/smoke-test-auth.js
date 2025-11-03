/*global console*/ 
/* eslint no-console: "off" */

import { check } from 'k6';
import exec from 'k6/execution'
import { fetchGoldenMacaroon, generateDPCToken } from './generate-dpc-token.js';
import {
  createClientToken,
  deleteOrganization,
  getOrganizationById,
  getOrganizationByAccessToken,
  createPublicKey,
  createSmokeTestOrganization,
  findOrganizationByNpi,
  retrieveAccessToken,
  validateJwt
} from './dpc-api-client.js';
import {
	 exportPublicKey,
	 generateKey,
	 makeJwt,
	 signatureSnippet
} from './generate-jwt.js'

import NPIGeneratorCache from './utils/npi-generator.js';

const npiGeneratorCache = new NPIGeneratorCache();

export const options = {
  thresholds: {
    checks: ['rate===1'],
  },
  scenarios: {
    checkAuthWorkflow: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: "checkAuthWorkflow"
    }
  }
};

// Sets up two test organizations
export function setup() {
  const goldenMacaroon = fetchGoldenMacaroon();
  const orgIds = [
    // '0ab352f1-2bf1-44c4-aa7a-3004a1ffef12',
    // '69c0d4d4-9c07-4fa8-9053-e10fb1608b48',
    'c7f5247b-4c41-478c-84eb-a6e801bdb145'
  ];
  const tokens = Array();
  const npiGenerator = npiGeneratorCache.getGenerator(0);
  // array returned from setup distributes its members starting from the 1 index
  for (let i = 0; i < orgIds.length; i++) {
    // delete smoke test org if present
    const orgId = orgIds[i];
    const token = generateDPCToken(orgId, goldenMacaroon);
    tokens[i] = token;
    const existingOrgResponse = getOrganizationById(token, orgId);

    const checkGetOrgOutput = check(
      existingOrgResponse,
      {
	'find org by id code 200 or 404': res => res.status == 404 || res.status == 200,
      }
    );
    
    if (!checkGetOrgOutput) {
      console.error(existingOrgResponse.body);
      exec.test.abort('failed find org by id');
    }

    if (existingOrgResponse == 200) {
      deleteOrganization(orgId, goldenMacaroon);
    }
    var npi;
    while(true) {
      npi = npiGenerator.iterate();
      // check if org with npi exists
      const existingNpiResponse = findOrganizationByNpi(npi, goldenMacaroon);

      const checkFindOutput = check(
	existingNpiResponse,
	{
          'find org by npi response code was 200': res => res.status === 200,
	}
      );

      if (!checkFindOutput) {
	console.error(existingNpiResponse.body);
	exec.test.abort('failed find org by npi');
      }
      
      const existingOrgs =  existingNpiResponse.json();

      if ( existingOrgs.total == 0) break;
    }

    const org = createSmokeTestOrganization(npi, orgId, goldenMacaroon);

    const checkOutput = check(
      org,
      {
        'create org response code was 200': res => res.status === 200,
        'create org response has id': res => res.json().id,
      }
    );

    if (!checkOutput) {
      console.error(org.body);
      exec.test.abort('failed to create organizations on setup')
    }

  }

  return { orgIds: orgIds, tokens: tokens, goldenMacaroon: goldenMacaroon };
}

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

export function teardown(data) {
  for (const orgId of data.orgIds) {
    if (orgId) {
      deleteOrganization(orgId, data.goldenMacaroon);
    }
  }
}
