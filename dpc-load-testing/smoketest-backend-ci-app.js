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
  createHeaderParam,
  createOrganization,
  createPatientsBatch,
  createPractitioners,
  deletePractitioner,
  deleteOrganization,
  exportGroup,
  findGroupByPractitionerNpi,
  findJobById,
  findOrganizationByNpi,
} from './dpc-api-client.js';

const npiGeneratorCache = new NPIGeneratorCache();

export function setup() {
  const goldenMacaroon = fetchGoldenMacaroon();
  const npiGenerator = npiGeneratorCache.getGenerator(0);
  const npi = npiGenerator.iterate();
  // check if org with npi exists
  const existingOrgResponse = findOrganizationByNpi(npi, goldenMacaroon);
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

  const practitionerNpi = '2459425221' // hard-coded for lookback tests

  return { orgId: org.json().id, goldenMacaroon: goldenMacaroon, practitionerNpi: practitionerNpi };
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

  console.log('running jmx smoketests for organization: ', orgId);
  const token = generateDPCToken(orgId, data.goldenMacaroon);

  // 1 of 4 (submitPractitioners)
  const practitionerNpi = '2459425221' // hard-coded for lookback tests
  const practitionerResponse = createPractitioners(token, practitionerNpi);
  console.log('practitionerResponse.json(): ', practitionerResponse.json());
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
  const memberContentVerified = function(res) {
    let pass = true;
    res.json().member.forEach((patient) => {
      if (!patients.includes(patient.entity.reference.slice(8))){
        pass = false;
      }
      if (!patient.period.start || patient.period.start === patient.period.end) {
        pass = false;
      }
    });
    return pass;
  }
  console.log('groupId: ', groupId);
  console.log('groupResponse.json(): ', groupResponse.json());

  check(
    groupResponse,
    {
      'response code was 201': res => res.status === 201,
      'accept header fhir type': res => res.headers['Content-Type'] === fhirType,
      'correct number of patients': res => res.json().member.length === mbis.length,
      'member content verified': memberContentVerified,
    }
  );
//export function createGroupWithPatients(token, orgId, practitionerId, practitionerNpi, patients) {
  // tbd
  // 4 of 4 (exportData)
  handleExportJob(token, groupId);
}
function handleExportJob(token, groupId) {
//  const getGroupExportResponseWithSince = exportGroup(token, groupId, `_since=${sinceDate}`);
  const getGroupExportResponseWithSince = exportGroup(token, groupId);
//  final Map<String, List<String>> headers = outcome.getResponseHeaders();
//  // Get the headers and check the status
//  final String exportURL = headers.get("content-location").get(0);
  console.log('status code: ', getGroupExportResponseWithSince.status);
  check(getGroupExportResponseWithSince, {
    'kickoff 202': r => r.status === 202,
    'has Content-Location': r => !!r.headers['Content-Location'],
  });
  console.log('full res', getGroupExportResponseWithSince);
  let exportJobURL = getGroupExportResponseWithSince.headers["Content-Location"];
  // e.g. http://localhost:3002/api/v1/Jobs/4c5ac919-bd2b-4194-9013-233b26363af9
  console.log('exportJobURL: ', exportJobURL);
  monitorExportJob(token, groupId, exportJobURL);
}

const getUuidFromUrl = (s) => {
  const m = s.match(/\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\b(?=\/?$)/i);
  return m ? m[0] : null;
};

const EXPORT_POLL_INTERVAL = 20000;
const EXPORT_POLL_TIMEOUT = 300000;
function monitorExportJob(token, groupId, jobLocationUrl) {
  const jobId = getUuidFromUrl(jobLocationUrl);
  console.log('handling jobId: ', jobId);
  const start = Date.now() / 1000;

  while (true) {
    const headers = createHeaderParam(token);
    const jobResponse = findJobById(jobId);
    const statusCode = jobResponse.status;

    // Typically 202 while running, 200 with JSON body when done.
    if (statusCode > 300) {
      throw new Error(`Export for ${groupId} failed with status code: ${statusCode}`);
    }
    else if (statusCode === 200) {
      // "done"
      check(res, {
        'job completed (200 code)': r => r.status === 200,
      });
      break;
    }

    const elapsed = (Date.now() - start) / 1000;
    if (elapsed > EXPORT_POLL_TIMEOUT) {
      throw new Error(`Export for ${groupId} timed out after ${EXPORT_POLL_TIMEOUT}s`);
    }

    sleep(EXPORT_POLL_INTERVAL / 1000);
  }
}

export function workflow(data) {
  // port from src/test/smoke_test.yml + src/main/resources/SmokeTest.jmx + src/main/java/gov/cms/dpc/testing/smoketests/SmokeTest.java
  handleJmxSmoketests(data);
}

export function teardown(data) {
  console.log('deleting organization: ', data.orgId);
  deleteOrganization(data.orgId, data.goldenMacaroon);
  console.log('organization deleted!');
  console.log('deleting practitioner: ', '751febad-4c59-4f04-ad10-af51f7b26cb6');
  deletePractitioner('751febad-4c59-4f04-ad10-af51f7b26cb6', data.goldenMacaroon)
  console.log('deleted practitioner');
//  const groupResponse = findGroupByPractitionerNpi(data.practitionerNpi);
//  const groupId = groupResponse.json().id;
//  console.log('groupResponse:', groupResponse.json());

//  console.log('deleting group:', groupId);
//  deleteGroup(token, groupId);
//  console.log('group deleted!');
}
