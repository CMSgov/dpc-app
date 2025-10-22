/*global console*/
/* eslint no-console: "off" */

import http from 'k6/http';
import { check } from 'k6';
import { sleep } from 'k6';
import exec from 'k6/execution'
import { fetchGoldenMacaroon, generateDPCToken } from './generate-dpc-token.js';
import NPIGeneratorCache from './utils/npi-generator.js';
import {
  createGroupWithPatients,
  createOrganization,
  createPatientsBatch,
  createPractitioners,
  deleteOrganization,
  findOrganizationByNpi,

} from './dpc-api-client.js';

const npiGeneratorCache = new NPIGeneratorCache();

export function setup() {
  const goldenMacaroon = fetchGoldenMacaroon();
  console.log("goldenMacaroon: ", goldenMacaroon);
  const npiGenerator = npiGeneratorCache.getGenerator(0);
  const npi = npiGenerator.iterate();
  console.log('npi: ', npi);
  // check if org with npi exists
  const existingOrgResponse = findOrganizationByNpi(npi, goldenMacaroon);
  console.log('existingOrgResponse:', existingOrgResponse);
  console.log('existingOrgResponse.status:', existingOrgResponse.status);
  console.log('existingOrgResponse.headers["Content-Type"]:', existingOrgResponse.headers["Content-Type"]);
  const checkFindOutput = check(
    existingOrgResponse,
    {
      'status OK and fhir header 1': fhirOK,
    }
  );

  if (!checkFindOutput) {
    exec.test.abort('failed to check for existing orgs');
  }
  // delete if org exists with npi
  const existingOrgs =  existingOrgResponse.json();
  if ( existingOrgs.total ) {
    for ( const entry of existingOrgs.entry ) {
      deleteOrganization(entry.resource.id, goldenMacaroon);
    }
  }

  const org = createOrganization(npi, `Test Org`, goldenMacaroon);

  const checkCreateOrganization = check(
    org,
    {
      'response code was 200': res => res.status === 200,
      'accept header fhir type': res => res.headers['Content-Type'] === fhirType,
      'response has id field': res => res.json().id != undefined,
      'id field is not null': res => res.json().id != null
    }
  );

  if (!checkCreateOrganization) {
    exec.test.abort('failed to create organizations on setup')
  }

  return { orgId: org.json().id, goldenMacaroon: goldenMacaroon };
}

export const options = {
  thresholds: {
    checks: ['rate===1'],
  },
  scenarios: {
    workflow: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: "workflow"
    }
  }
};

const fhirType = 'application/fhir+json';
const fhirOK = function(res) {
  return res.status === 200 && res.headers['Content-Type'] === fhirType;
};

function handleJmxSmoketests(data) {
  console.log('handle jmx tests...')
  // COPIED from dpc-load-testing/ci-app.js
  // move to shared util..
  // hard-coded to ensure proper data retrieval
  const mbis = ['1SQ3F00AA00', '5S58A00AA00', '4S58A00AA00', '3S58A00AA00', '0S80C00AA00']
  const orgId = data.orgId;

  const token = generateDPCToken(orgId, data.goldenMacaroon);

  // 1 of 4 (submitPractitioners)
  const practitionerNpi = '2459425221' // hard-coded for lookback tests
  const practitionerResponse = createPractitioners(token, practitionerNpi);
  const checkPractitionerResponse = check(
    practitionerResponse,
    {
      'status OK and fhir header 2': fhirOK,
      'practitioner id an npi': res => res.json().entry[0].resource.identifier[0].system === 'http://hl7.org/fhir/sid/us-npi',
    }
  );

  let practitionerId;
  if(checkPractitionerResponse) {
    // There's only 1 identifier in our synthetic practitioner, so we don't have to search for npi
    practitionerId = practitionerResponse.json().entry[0].resource.id;
    console.log(practitionerId);
  } else {
    console.error('failed to create practitioners');
  }

  // 2 of 4 (submitPractitioners)
  // POST patients
  const patientsResponse = createPatientsBatch(token, mbis);
  const checkPatientsResponse = check(
    patientsResponse,
    {
      'status OK and fhir header 3': fhirOK,
      'created patients': res => res.json().entry.length === mbis.length,
    }
  );
  const patients = [];
  if(checkPatientsResponse) {
    patientsResponse.json().entry.forEach((entry) => patients.push(entry.resource.id));
  } else {
    console.error('failed to create patients');
  }
  console.log(checkPatientsResponse);
  // 3 of 4 (submitRosters)
  const groupResponse = createGroupWithPatients(token, orgId, practitionerId, practitionerNpi, patients);
  const groupId = groupResponse.json().id;
//export function createGroupWithPatients(token, orgId, practitionerId, practitionerNpi, patients) {
  // tbd
  // 4 of 4 (exportData)
  // tbd
  // exportGroup
  handleExportJob(token, groupId)
}
function handleExportJob(token, groupId) {
//  const getGroupExportResponseWithSince = exportGroup(token, groupId, `_since=${sinceDate}`);
  const getGroupExportResponseWithSince = exportGroup(token, groupId);
//  final Map<String, List<String>> headers = outcome.getResponseHeaders();
//  // Get the headers and check the status
//  final String exportURL = headers.get("content-location").get(0);
  console.log('look at export url headers: ', getGroupExportResponseWithSince.headers);
  let exportJobURL = getGroupExportResponseWithSince.headers["content-location"][0];
  monitorExportJob(exportJobURL, 'asdf');
}

const EXPORT_POLL_INTERVAL = 20000;
const EXPORT_POLL_TIMEOUT = 300000;
function monitorExportJob(jobLocationUrl, groupId) {
    const start = Date.now() / 1000;

    while (true) {
      //  move to dpc-api-client
      jobResponse = http.get(jobLocationUrl, authHeaders());
      statusCode = jobResponse.status;

      // Typically 202 while running, 200 with JSON body when done.
      if (statusCode > 300) {
        throw new Error(`Export for ${group.id} failed with status code: ${statusCode}`);
      }
      else if (statusCode === 200) {
        // "done"
        return
      }
      check(statusRes, { 'still running or ok': r => r.status === 202 || r.status === 200 });

      const elapsed = (Date.now() - start) / 1000;
      if (elapsed > EXPORT_POLL_TIMEOUT) {
        throw new Error(`Export for ${group.id} timed out after ${EXPORT_POLL_TIMEOUT}s`);
      }

      sleep(EXPORT_POLL_INTERVAL / 1000);
    }
}

export function workflow(data) {
  // port from src/test/smoke_test.yml + src/main/resources/SmokeTest.jmx + src/main/java/gov/cms/dpc/testing/smoketests/SmokeTest.java
  handleJmxSmoketests(data);
}
