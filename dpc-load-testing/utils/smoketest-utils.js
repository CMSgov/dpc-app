/*global console*/
/* eslint no-console: "off" */

import { check  } from 'k6';
import exec from 'k6/execution'
import { fetchGoldenMacaroon, generateDPCToken } from '../generate-dpc-token.js';
import {
  deleteOrganization,
  getOrganizationById,
  createSmokeTestOrganization,
  findOrganizationByNpi,
} from '../dpc-api-client.js';
import NPIGeneratorCache from './npi-generator.js';

const npiGeneratorCache = new NPIGeneratorCache();

export function setupSmokeTests() {
  const goldenMacaroon = fetchGoldenMacaroon();
  const orgIds = [
    '0ab352f1-2bf1-44c4-aa7a-3004a1ffef12',
    '69c0d4d4-9c07-4fa8-9053-e10fb1608b48',
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
      exec.test.abort(`failed find org by id: ${orgId}`);
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

export function tearDownSmokeTests(data) {
  for (const orgId of data.orgIds) {
    if (orgId) {
      deleteOrganization(orgId, data.goldenMacaroon);
    }
  }
}
