import http from 'k6/http';
import { sleep } from 'k6';

// See https://grafana.com/docs/k6/latest/using-k6/k6-options/reference for
// details on this configuration object.
export const options = {
  // A number specifying the number of VUs to run concurrently.
  vus: 10,
  // A string specifying the total duration of the test run.
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01']
  }
};

export default function() {
  http.get('https://test.k6.io');
  sleep(1);
}
