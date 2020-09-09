import { getBaseUrl } from './modules/config.js';
import http from 'k6/http';
import { check, fail } from 'k6';

let baseUrl = getBaseUrl();

export let options = {
  thresholds: {
    http_req_duration: ['p(95)<500'],
  },
};

export default function() {
  let resp = http.get(baseUrl + '/v1/metadata');
  if (!check(resp, {
    "Response status should be 200 OK": (resp) => resp.status == 200
  })) {
    fail(`Response status should have been 200 but was ${resp.status}`)
  }
}
