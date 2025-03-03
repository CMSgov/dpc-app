import { check, sleep } from 'k6';
import http from 'k6/http';
import exec from 'k6/execution'
import generateDPCToken from './generate-dpc-token.js';
import { createOrganization } from './dpc-api-client.js';

// See https://grafana.com/docs/k6/latest/using-k6/k6-options/reference for
// details on this configuration object.
export const options = {
  // A number specifying the number of VUs to run concurrently.
  vus: 1,
  // A string specifying the total duration of the test run.
  duration: '10s',
  thresholds: {
    http_req_failed: ['rate<0.01']
  }
};

let org1Id;
let org2Id;

// Sets up two test organizations and saves their UUIDs for use later.
export function setup() {
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

  org1Id = org1.json().id;
  org2Id = org2.json().id;
}

let bearerToken;

export default function() {
  if (!bearerToken) {
    const tokenResponse = generateDPCToken();
    if (tokenResponse.status.toString() == '200') {
      bearerToken = tokenResponse.body;
      console.log('bearer token fetched successfully!');
    } else {
      console.error('failed to fetch bearer token');
    }
  }

  http.get('https://test.k6.io');
  sleep(1);
}
