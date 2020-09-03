import { getBaseUrl } from './modules/config.js';
import http from 'k6/http';
import { check, fail, sleep } from 'k6';

let baseUrl = getBaseUrl();

export let options = {
  vus: 10,
  duration: '10s',
};

export default function() {
  let resp = http.get(baseUrl + '/v1/metadata');
  if (!check(resp, {
    "Response status should be 200 OK": (resp) => resp.status == 200
  })) {
    fail(`Response status should have been 200 but was ${resp.status}`)
  }
  sleep(1);
}
