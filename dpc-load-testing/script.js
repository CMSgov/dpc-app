import { check, fail } from 'k6';
import exec from 'k6/execution'
import tokenCache, { generateDPCToken, fetchGoldenMacaroon } from './generate-dpc-token.js';
import { createOrganization, deleteOrganization, getOrganization } from './dpc-api-client.js';

// See https://grafana.com/docs/k6/latest/using-k6/k6-options/reference for
// details on this configuration object.
export const options = {
  scenarios: {
    workflow_a: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: "workflowA"
    },
    workflow_b: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: "workflowB"
    }
  }
};

let goldenMacaroon;

// Sets up two test organizations
export function setup() {
  goldenMacaroon = fetchGoldenMacaroon();
  tokenCache.setGoldenMacaroon(goldenMacaroon);
  // Fake NPIs generated online: https://jsfiddle.net/alexdresko/cLNB6
  const org1 = createOrganization('2782823019', 'Test Org 1');
  const org2 = createOrganization('8197402604', 'Test Org 2');

  const checkOutput1 = check(
    org1,
    { 
      'response code was 200': res => res.status === 200,
      'response has id field': res => res.json().hasOwnProperty('id'),
      'id field is not null or undefined': res => res.json().id != null && res.json().id != undefined 
    }
  );
  const checkOutput2 = check(
    org2,
    { 
      'response code was 200': res => res.status === 200,
      'response has id field': res => res.json().hasOwnProperty('id'),
      'id field is not null or undefined': res => res.json().id != null && res.json().id != undefined
    }
  )

  if (!checkOutput1 || !checkOutput2) {
    exec.test.abort('failed to create organizations on setup')
  }

  // array returned from setup distributes its members starting from the 1 index
  const orgIds = Array();
  orgIds[1] = org1.json().id;
  orgIds[2] = org2.json().id;

  return orgIds;
}

export function workflowA(data) {
  const orgId = data[exec.vu.idInInstance];
  const tokenResponse = generateDPCToken(orgId);
  if (tokenResponse.status.toString() == '200') {
    tokenCache.setToken(orgId, tokenResponse.body);
    console.log('bearer token for workflow A fetched successfully!');
  } else {
    console.error('failed to fetch bearer token for workflow A');
  }
  
  const orgResponse = getOrganization(orgId);
  const checkOutput = check(
    orgResponse, 
    { 'response code was 200': res => res.status === 200 }
  )

  if (!checkOutput) {
    fail('Failed to get a 200 response in workflow A');
  }
}

export function workflowB(data) {
  const orgId = data[exec.vu.idInInstance];
  const tokenResponse = generateDPCToken(orgId);
  if (tokenResponse.status.toString() == '200') {
    tokenCache.setToken(orgId, tokenResponse.body);
    console.log('bearer token for workflow B fetched successfully!');
  } else {
    console.error('failed to fetch bearer token for workflow B');
  }

  const orgResponse = getOrganization(orgId);
  const checkOutput = check(
    orgResponse, 
    { 'response code was 200': res => res.status === 200 }
  )

  if (!checkOutput) {
    fail('Failed to get a 200 response in workflow B');
  }
}

export function teardown(data) {
  for (const orgId of data) {
    if (orgId) {
      deleteOrganization(orgId);
    }
  }
}
