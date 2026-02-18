/*global console*/
/* eslint no-console: "off" */

import { check } from 'k6';
import exec from 'k6/execution'
import { fetchGoldenMacaroon, generateDPCToken } from './generate-dpc-token.js';
import {
  deleteOrganization,
  getOrganizationById,
  createSmokeTestOrganization,
  findOrganizationByNpi,
} from './dpc-api-client.js';
import { checkBulkExportWorkflow } from './smoke_test_workflows/bulk_export.js';
import { checkAuthWorkflow } from './smoke_test_workflows/auth.js';
import { checkPortalsWorkflow } from './smoke_test_workflows/portals.js';
import { checkPatientEverythingExportWorkflow } from './smoke_test_workflows/patient_export.js';

import NPIGeneratorCache from './utils/npi-generator.js';

const npiGeneratorCache = new NPIGeneratorCache();

export const options = {
  thresholds: {
    checks: ['rate===1'],
  },
  scenarios: {
    smokeTests: {
      executor: 'per-vu-iterations',
      vus: 3, // This has to stay at three to ensure even distribution of organizations.
      iterations: 1,
      exec: "runSmokeTests"

    }
  }
};

const orgIds = [
  '0ab352f1-2bf1-44c4-aa7a-3004a1ffef12',
  '69c0d4d4-9c07-4fa8-9053-e10fb1608b48',
  'c7f5247b-4c41-478c-84eb-a6e801bdb145'
];

export function runSmokeTests(data) {
  const idx = exec.vu.idInInstance % 3;
  const iterationData = {
      idx: idx,
      orgId: orgIds[idx],
      goldenMacaroon: data.goldenMacaroon
  };
  checkAuthWorkflow(iterationData);
  checkBulkExportWorkflow(iterationData);
  checkPortalsWorkflow(iterationData);
  checkPatientEverythingExportWorkflow(iterationData);
}

// Sets up three test organizations
export function setup() {
  const goldenMacaroon = fetchGoldenMacaroon();
  const npiGenerator = npiGeneratorCache.getGenerator(0);
  // array returned from setup distributes its members starting from the 1 index
  for (let i = 0; i < orgIds.length; i++) {
    // delete smoke test org if present
    const orgId = orgIds[i];
    const token = generateDPCToken(orgId, goldenMacaroon);
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

    if (existingOrgResponse.status == 200) {
      deleteOrganization(orgId, goldenMacaroon);
    }
    let npi;
    let count = 0;
    while(count < 200) {
      count += 1;
      const npiToCheck = npiGenerator.iterate();
      // check if org with npi exists
      const existingNpiResponse = findOrganizationByNpi(npiToCheck, goldenMacaroon);

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

      if ( existingOrgs.total == 0) {
        npi = npiToCheck;
        break;
      }
    }
    if (! npi) {
      exec.test.abort('failed to generate unused npi');
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

  return { orgIds: orgIds, goldenMacaroon: goldenMacaroon };
}

export function teardown(data) {
  for (const orgId of data.orgIds) {
    if (orgId) {
      deleteOrganization(orgId, data.goldenMacaroon);
    }
  }
}
