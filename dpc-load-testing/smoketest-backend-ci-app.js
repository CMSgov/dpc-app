/*global console*/
/* eslint no-console: "off" */

import { check, fail, sleep } from 'k6';
import { fhirOK, getUuidFromUrl, memberContentVerified } from './utils/test-utils.js';
import { setupSmokeTests, tearDownSmokeTests } from './utils/smoketest-utils.js';
import {
  createGroupWithPatients,
  createPatientsBatch,
  createPractitioners,
  deletePractitioner,
  exportGroup,
  findJobById,
} from './dpc-api-client.js';

const EXPORT_POLL_INTERVAL_SEC = 15;
const EXPORT_POLL_TIMEOUT_SEC = 600;

export const options = {
  thresholds: {
    checks: ['rate===1'],
  },
  scenarios: {
    backendWorkflow: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: "backendWorkflow"
    },
  }
};

export function setup() {
  return setupSmokeTests();
}

function handleJmxSmoketests(data) {
  console.log('handle jmx tests...')
  const mbis = ['1SQ3F00AA00', '5S58A00AA00', '4S58A00AA00', '3S58A00AA00', '0S80C00AA00'];
  data.orgIds.forEach((orgId, index) => {
    const token = data.tokens[index];

    // 1 of 4 (submitPractitioners)
    const practitionerNpi = '2459425221' // hard-coded for lookback tests
    const practitionerResponse = createPractitioners(token, practitionerNpi);
    const checkPractitionerResponse = check(
      practitionerResponse,
      {
        'status OK and fhir header': fhirOK,
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

    // 2 of 4 (submitPatients)
    const patientsResponse = createPatientsBatch(token, mbis);
    const checkPatientsResponse = check(
      patientsResponse,
      {
        'status OK and fhir header': fhirOK,
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

    check(
      groupResponse,
      {
        'status OK and fhir header': fhirOK,
        'correct number of patients': res => res.json().member.length === mbis.length,
        'member content verified': res => memberContentVerified(res, patients),
      }
    );
    // 4 of 4 (exportData)
    handleExportJob(token, groupId);

    // using "real" practitioner endpoint instead of .json file
    // needs to be cleaned up before tearDown()
    const deletePractitionerResponse = deletePractitioner(token, practitionerId);
    check(
      deletePractitionerResponse,
      {
        'delete practitioner response code was 200': res => res.status === 200,
      }
    );
  });
}

function handleExportJob(token, groupId) {
  const getGroupExportResponseWithSince = exportGroup(token, groupId);
  check(getGroupExportResponseWithSince, {
    'kickoff 202': r => r.status === 202,
    'has Content-Location': r => !!r.headers['Content-Location'],
  });
  console.log('full res', getGroupExportResponseWithSince);
  let exportJobURL = getGroupExportResponseWithSince.headers["Content-Location"];
  monitorExportJob(token, groupId, exportJobURL);
}


// ported from ClientUtils.awaitExportResponse
function monitorExportJob(token, groupId, jobLocationUrl) {
  const jobId = getUuidFromUrl(jobLocationUrl);
  console.log('handling jobId: ', jobId);
  const start = Date.now();

  while (true) {
    const jobResponse = findJobById(token, jobId);
    const statusCode = jobResponse.status;

    if (statusCode > 300) {
      fail(`Export for ${groupId} failed with status code: ${statusCode}`);
    }
    else if (statusCode === 200) {
      check(jobResponse, {
        'job completed (200 code)': r => r.status === 200,
      });
      break;
    }

    const elapsed_sec = (Date.now() - start) / 1000;
    if (elapsed_sec > EXPORT_POLL_TIMEOUT_SEC) {
      console.log('reached poll timeout');
      fail('status code was *not* 200');
    }

    sleep(EXPORT_POLL_INTERVAL_SEC);
  }
}

export function backendWorkflow(data) {
  // port from src/test/smoke_test.yml + src/main/resources/SmokeTest.jmx + src/main/java/gov/cms/dpc/testing/smoketests/SmokeTest.java
  handleJmxSmoketests(data);
}

export function teardown(data) {
  return tearDownSmokeTests(data);
}
