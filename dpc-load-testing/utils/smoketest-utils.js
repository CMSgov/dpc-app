/*global console*/
/* eslint no-console: "off" */

import { check, fail  } from 'k6';
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

// from patient_bundle-dpr.json
export function getSmoketestNonprodMBIs() {
  return ['1S00E00FZ00', '1S00E00AA02', '1S00E00AA03', '1S00E00AA01', '1S00E00AA04', '1S00E00AA05', '1S00E00AA06', '1S00E00AA07', '1S00E00AA08', '1S00E00AA09', '1S00E00AA10', '1S00E00AA11', '1S00E00AA12', '1S00E00AA13', '1S00E00AA14', '1S00E00AA15', '1S00E00AA16', '1S00E00AA17', '1S00E00AA18', '1S00E00AA19', '1S00E00AA20', '1S00E00AA21', '1S00E00AA22', '1S00E00AA23', '1S00E00AA24', '1S00E00AA25', '1S00E00AA26', '1S00E00AA27', '1S00E00AA28', '1S00E00AA29', '1S00E00AA30', '1S00E00AA31', '1S00E00AA32', '1S00E00AA33', '1S00E00AA34', '1S00E00AA35', '1S00E00AA36', '1S00E00AA37', '1S00E00AA38', '1S00E00AA39', '1S00E00AA40', '1S00E00AA41', '1S00E00AA42', '1S00E00AA43', '1S00E00AA44', '1S00E00AA45', '1S00E00AA46', '1S00E00AA47', '1S00E00AA48', '1S00E00AA49', '1S00E00AA50', '1S00E00AA51', '1S00E00AA52', '1S00E00AA53', '1S00E00AA54', '1S00E00AA55', '1S00E00AA56', '1S00E00AA57', '1S00E00AA58', '1S00E00AA59', '1S00E00AA60', '1S00E00AA61', '1S00E00AA62', '1S00E00AA63', '1S00E00AA64', '1S00E00AA65', '1S00E00AA66', '1S00E00AA67', '1S00E00AA68', '1S00E00AA69', '1S00E00AA70', '1S00E00AA71', '1S00E00AA72', '1S00E00AA73', '1S00E00AA74', '1S00E00AA75', '1S00E00AA76', '1S00E00AA77', '1S00E00AA78', '1S00E00AA79', '1S00E00AA80', '1S00E00AA81', '1S00E00AA82', '1S00E00AA83', '1S00E00AA84', '1S00E00AA85', '1S00E00AA86', '1S00E00AA87', '1S00E00AA88', '1S00E00AA89', '1S00E00AA90', '1S00E00AA91', '1S00E00AA92', '1S00E00AA93', '1S00E00AA94', '1S00E00AA95', '1S00E00AA96', '1S00E00AA97', '1S00E00AA98', '1S00E00AA99'];
}

// from prod_patient_bundle-dpr.json
export function getSmoketestProdMBIs() {
  return ['1SQ3F00AA00', '5S58A00AA00', '4S58A00AA00', '3S58A00AA00', '7S48A00AA00', '3S51C00AA00', '6S48A00AA00', '4S51C00AA00', '5S48A00AA00', '2S58A00AA00', '8S41C00AA00', '1S58A00AA00', '9S41C00AA00', '6SK4F00AA00', '9S48A00AA00', '1S51C00AA00', '8S48A00AA00', '2S51C00AA00', '4S41C00AA00', '5S41C00AA00', '6S41C00AA00', '7S41C00AA00', '8SR0C00AA00', '4SE4F00AA00', '7SR0C00AA00', '6SR0C00AA00', '6SE4F00AA00', '5SR0C00AA00', '5SE4F00AA00', '3ST0C00AA00', '2ST0C00AA00', '8S95D00AA00', '1ST0C00AA00', '9SR0C00AA00', '5S95D00AA00', '4SR3F00AA00', '4S95D00AA00', '5SR3F00AA00', '7S95D00AA00', '6SR3F00AA00', '6S95D00AA00', '7SR3F00AA00', '4SR0C00AA00', '1S95D00AA00', '3SR0C00AA00', '9S85D00AA00', '3S95D00AA00', '2S95D00AA00', '3SR3F00AA00', '3ST3F00AA00', '8SR3F00AA00', '9SR3F00AA00', '1ST3F00AA00', '2ST3F00AA00', '2SR0C00AA00', '6SQ0C00AA00', '5SQ0C00AA00', '4SQ0C00AA00', '3SQ0C00AA00', '1SR0C00AA00', '9SQ0C00AA00', '8SQ0C00AA00', '7SQ0C00AA00', '2SQ3F00AA00', '7S79E00AA00', '3SQ3F00AA00', '4SQ3F00AA00', '5SQ3F00AA00', '1SR3F00AA00', '2SR3F00AA00', '6SQ3F00AA00', '7SQ3F00AA00', '8SQ3F00AA00', '9SQ3F00AA00', '8SG4F00AA00', '7SG4F00AA00', '1SH4F00AA00', '8S80F00AA00', '9SG4F00AA00', '9S80F00AA00', '6SG4F00AA00', '5SG4F00AA00', '3S80F00AA00', '4SG4F00AA00', '4S80F00AA00', '5S80F00AA00', '6S80F00AA00', '1SG4F00AA00', '5SU3F00AA00', '4S79E00AA00', '9SF4F00AA00', '5S79E00AA00', '3SG4F00AA00', '6S79E00AA00', '2SG4F00AA00', '4SR8A00AA00', '3SR8A00AA00', '7S80F00AA00', '6SF4F00AA00', '5SF4F00AA00'];
}

function generateNpiForSmoketestOrganization(goldenMacaroon) {
  const npiGenerator = npiGeneratorCache.getGenerator(0);
  const MAX_NPI_ATTEMPTS = 200;
  let npi;

  for (let attempt = 0; attempt < MAX_NPI_ATTEMPTS; attempt++) {
    const npiAttempt = npiGenerator.iterate();
    // check if org with npi exists
    const existingNpiResponse = findOrganizationByNpi(npiAttempt, goldenMacaroon);

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

    const existingOrgs = existingNpiResponse.json();

    if ( existingOrgs.total == 0) {
      console.log(`generated npi for smoketest organization in ${attempt} attempts.`)
      npi = npiAttempt;
      break;
    }
  }
  if (!npi) {
    fail(`failed to generate npi within ${MAX_NPI_ATTEMPTS} attempts`);
  }
  return npi;
}

export function setupSmokeTests() {
  const goldenMacaroon = fetchGoldenMacaroon();
  const orgIds = [
    '0ab352f1-2bf1-44c4-aa7a-3004a1ffef12',
    '69c0d4d4-9c07-4fa8-9053-e10fb1608b48',
    'c7f5247b-4c41-478c-84eb-a6e801bdb145'
  ];

  const tokens = Array();
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


    const npi = generateNpiForSmoketestOrganization(goldenMacaroon);
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
