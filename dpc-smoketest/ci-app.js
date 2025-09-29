import http from 'k6/http';
import { check } from 'k6';
import { fetchGoldenMacaroon, generateDPCToken } from './generate-dpc-token.js';

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

function handleJmxSmoketests(hostUrl, adminUrl, seedFile) {
  // host = ${HOST}
  // admin-url = ${ADMIN_URL}
  // seed-file = ${SEED_FILE}
  // provider-bundle = ${PROVIDER_BUNDLE}
  // patient-bundle = ${PATIENT_BUNDLE}
  // organization-ids = "0ab352f1-2bf1-44c4-aa7a-3004a1ffef12,69c0d4d4-9c07-4fa8-9053-e10fb1608b48,c7f5247b-4c41-478c-84eb-a6e801bdb145"

  // COPIED from dpc-load-testing/ci-app.js
  // hard-coded to ensure proper data retrieval
  const mbis = ['1SQ3F00AA00', '5S58A00AA00', '4S58A00AA00', '3S58A00AA00', '0S80C00AA00']
  const orgId = data.orgId;

  const token = generateDPCToken(orgId, data.goldenMacaroon);

  // 1 of 5 (submitPractitioners)
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
  } else {
    console.error('failed to create practitioners');
  }

  // 2 of 5 (submitPractitioners)
  // POST patients
  const patientsResponse = createPatientsBatch(token, mbis);
  const checkPatientsResponse = check(
    patientsResponse,
    {
      'status OK and fhir header': fhirOK,
      'created patients': res => res.json().entry.length === mbis.length,
    }
  );
  // 3 of 5 (submitRosters)
  // 4 of 5 (exportData)
}

export function workflow() {
  // port from src/test/portal_test.yml
  checkLoginPage(__ENV.PORTAL_HOST, ["/portal", "/portal/organizations"], "Sign in");
  // port from src/test/web_test.yml
  checkLoginPage(__ENV.WEB_HOST, ["/users/sign_in", "/"], "Log in");
  // port from src/test/web_admin_test.yml
  checkLoginPage(__ENV.WEB_ADMIN_HOST, ["/admin/internal/sign_in", "/admin/organizations"], "Log in");

  // port from src/test/smoke_test.yml + src/main/resources/SmokeTest.jmx + src/main/java/gov/cms/dpc/testing/smoketests/SmokeTest.java
  handleJmxSmoketests();
}

