/*global console*/
/* eslint no-console: "off" */

import { check } from 'k6';
import exec from 'k6/execution';
import { fetchGoldenMacaroon, generateDPCToken } from './generate-dpc-token.js';
import {
  createSmokeTestOrganization,
  getOrganizationById,
} from './dpc-api-client.js';

export const options = {
  vus: 1,
  iterations: 1,
};

// Synthetic, persistent test fixtures added to lookBackExemptOrgs in application.yml.
// These UUIDs are generated test IDs
const syntheticTestOrganizations = [
  {
    id: 'a3abaf86-2cd4-4a32-a57e-1bda741ed00d',
    npi: '0009000122',
    name: 'Persistent Test Org - Login.gov',
  },
  {
    id: '66e9f10c-31c7-41a4-b88f-4d10e59432d7',
    npi: '0009000239',
    name: 'Persistent Test Org - ID.me',
  },
  {
    id: '97509c9f-4350-4b4f-a9d4-aba4dadffe1c',
    npi: '0009000346',
    name: 'Persistent Test Org - CLEAR',
  },
];

export default function () {
  const goldenMacaroon = fetchGoldenMacaroon();

  for (const organization of syntheticTestOrganizations) {
    const token = generateDPCToken(organization.id, goldenMacaroon);
    const existingOrgResponse = getOrganizationById(token, organization.id);

    if (existingOrgResponse.status == 200) {
      const existingOrganization = existingOrgResponse.json();
      if (!existingOrganization.identifier.some(identifier => identifier.value === organization.npi)) {
        console.error(existingOrgResponse.body);
        exec.test.abort(`org ${organization.id} exists but does not match npi ${organization.npi}`);
      }
      console.log(`org already exists: ${organization.id}`);
      continue;
    }

    if (existingOrgResponse.status != 404) {
      console.error(existingOrgResponse.body);
      exec.test.abort(`failed to check org ${organization.id}`);
    }

    const org = createSmokeTestOrganization(organization.npi, organization.id, goldenMacaroon);
    const checkOutput = check(
      org,
      {
        'create org response code was 200': res => res.status === 200,
        'create org response has expected id': res => res.json().id === organization.id,
        'create org response has expected npi': res => (
          res.json().identifier.some(identifier => identifier.value === organization.npi)
        ),
      }
    );

    if (!checkOutput) {
      console.error(org.body);
      exec.test.abort('failed to create organizations on setup');
    } else {
      console.log(`org created: ${organization.id}`);
    }
  }
}
