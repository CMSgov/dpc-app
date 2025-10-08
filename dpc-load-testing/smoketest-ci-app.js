/*global console*/
/* eslint no-console: "off" */

import http from 'k6/http';
import { check } from 'k6';
import { generateDPCToken } from './generate-dpc-token.js';
import {
  createPatientsBatch,
  createPractitioners,
} from './dpc-api-client.js';
import { setup as integrationTestSetup } from './ci-app.js';

export function setup() {
  return integrationTestSetup();
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

function checkLoginPage(baseUrl, paths, loginText) {
  if (!baseUrl) {
    throw new Error(`${baseUrl} environment variable is not set`)
  }

  paths.forEach(path => {
    const fullUrl = baseUrl + path;
    const res = http.get(fullUrl);
    console.log('checking url: ', fullUrl);
    console.log("res.status: ", res.status);

    check(res, {
      "is status 200": (r) => r.status === 200,
      "verify homepage text": (r) => r.body.includes("Data at the Point of Care"),
      "verify login text": (r) => r.body.includes(loginText)
    });
  })
}

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

  // 2 of 4 (submitPractitioners)
  // POST patients
  const patientsResponse = createPatientsBatch(token, mbis);
  const checkPatientsResponse = check(
    patientsResponse,
    {
      'status OK and fhir header': fhirOK,
      'created patients': res => res.json().entry.length === mbis.length,
    }
  );
  console.log(checkPatientsResponse);
  // 3 of 4 (submitRosters)
  // tbd
  // 4 of 4 (exportData)
  // tbd
}

export function workflow(data) {
//  HOST: ${HOST_URL}
//  PORTAL_HOST: ${PORTAL_HOST}
//  WEB_HOST: ${WEB_HOST}
//  WEB_ADMIN_HOST: ${WEB_ADMIN_HOST}
//  ADMIN_URL: ${ELB_URL}
  // port from src/test/portal_test.yml
  checkLoginPage(__ENV.PORTAL_HOST, ["/portal", "/portal/organizations"], "Sign in");
  // // port from src/test/web_test.yml
   checkLoginPage(__ENV.WEB_HOST, ["/users/sign_in", "/"], "Log in");
  // // port from src/test/web_admin_test.yml
   checkLoginPage(__ENV.WEB_ADMIN_HOST, ["/admin/internal/sign_in", "/admin/organizations"], "Log in");

  // port from src/test/smoke_test.yml + src/main/resources/SmokeTest.jmx + src/main/java/gov/cms/dpc/testing/smoketests/SmokeTest.java
  handleJmxSmoketests(data);
}
