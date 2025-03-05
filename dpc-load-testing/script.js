import http from 'k6/http';
import exec from 'k6/execution';
import { sleep } from 'k6';
import generateDPCToken from './generate-dpc-token.js';

// See https://grafana.com/docs/k6/latest/using-k6/k6-options/reference for
// details on this configuration object.
const adminUrl = __ENV.ENVIRONMENT == 'local' ? 'http://host.docker.internal:9903' : __ENV.API_ADMIN_URL;
const apiUrl = __ENV.ENVIRONMENT == 'local' ? 'http://host.docker.internal:3002/v1' : __ENV.API_URL;
const fetchGoldenMacaroonURL = `${adminUrl}/tasks/generate-token`;
const vusPerWorkflow = 1;

export const options = {
  scenarios: {
    workflow_a: {
      executor: 'per-vu-iterations',
      vus: vusPerWorkflow,
      iterations: 1,
      exec: "workflowA"
    },
    workflow_b: {
      executor: 'per-vu-iterations',
      vus: vusPerWorkflow,
      iterations: 1,
      exec: "workflowB"
    }
  }
};

export function setup() {
  const npis = ['3359188240', '3713518470']
  let macaroon = http.post(fetchGoldenMacaroonURL, {});
  let orgIds = Array();
  for (let i = 0; i < npis.length; i++) {
    orgIds[i + 1] = makeOrg(npis[i], macaroon.body);
  }
  return orgIds;
}

export function setupx() {
  return ['404e1d6b-71e5-4900-bae8-8d698f5efc96', '3951088b-f544-4d9c-b3d4-b7ebcf6ae67f']
}
export function workflowA(data) {
  console.log('Running Workflow A ' + data[exec.vu.idInInstance]);
  sleep(1);
}
export function workflowB(data) {
  let orgId = data[exec.vu.idInInstance];
  console.log('Running Workflow B ' + data[exec.vu.idInInstance]);
  let token = generateDPCToken(orgId);
  console.log(token);
  let headers = { 'Content-Type': 'application/fhir+json', Accept: 'application/fhir+json', 'Authorization': 'Bearer ' + token };
  let response = http.get(apiUrl + '/Organization/' + orgId, { headers: headers });
  console.log(response);
}

export function teardown(data) {
  let macaroon = http.post(fetchGoldenMacaroonURL, {});
  for (let i = 0; i < data.length; i++) {
    let orgId = data[i];
    if (orgId) {
      deleteOrg(orgId, macaroon.body);
    }
  }
}


function makeOrg(npi, macaroon) {
  let orgData = {"resourceType":"Parameters","parameter":[{"name":"resource","resource":{"resourceType":"Bundle","type":"collection","entry":[{"resource":{"address":[{"use":"work","type":"both","city":"Newark","country":"US","line":["21 Jump ST","Office 15"],"postalCode":"22222","state":"NJ"}],"identifier":[{"system":"http://hl7.org/fhir/sid/us-npi","value":npi}],"name":"Load Testing Org " + npi,"resourceType":"Organization","type":[{"coding":[{"code":"prov","display":"Healthcare Provider","system":"http://hl7.org/fhir/organization-type"}],"text":"Healthcare Provider"}]}}]}}]};
  let headers = { 'Content-Type': 'application/fhir+json', Accept: 'application/fhir+json', 'Authorization': 'Bearer ' + macaroon };
  let response = http.post(apiUrl + '/Organization/$submit', JSON.stringify(orgData), { headers: headers });
  let org = JSON.parse(response.body);
  return org['id'];
}
function deleteOrg(orgId, macaroon) {
  let headers = { 'Authorization': 'Bearer ' + macaroon };
  let response = http.del(apiUrl + '/Organization/' + orgId, null, { headers: headers });
}
