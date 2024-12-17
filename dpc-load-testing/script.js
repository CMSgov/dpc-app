import http from 'k6/http';
import { sleep } from 'k6';
import generateDPCToken from './generate-dpc-token.js';

// See https://grafana.com/docs/k6/latest/using-k6/k6-options/reference for
// details on this configuration object.
export const options = {
  // A number specifying the number of VUs to run concurrently.
  vus: 1,
  // A string specifying the total duration of the test run.
  duration: '1m',
  thresholds: {
    http_req_failed: ['rate<0.01']
  }
};

let bearerToken;

export default function() {
  console.log('starting run')
  if (!bearerToken) {
    const tokenResponse = generateDPCToken();
    if (tokenResponse.status.toString() == '200') {
      bearerToken = tokenResponse.json()['access_token'];
      console.log('bearer token fetched successfully!');
    } else {
      console.error('failed to fetch bearer token')
      console.log(tokenResponse.status)
      console.log(tokenResponse.html())
    }
  }

  http.get('https://test.k6.io');
  sleep(1);
}
