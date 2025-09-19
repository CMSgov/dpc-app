import http from 'k6/http';
import { check } from 'k6';

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


export default function () {
  const baseUrl = __ENV.WEB_HOST;
  if (!baseUrl) {
    throw new Error("WEB_HOST environment variable is not set");
  }

  const url = `${baseUrl}/users/sign_in`;
  const res = http.get(url);

  check(res, {
    "status is 200": (r) => r.status === 200,
    "body has 'Data at the Point of Care'": (r) => r.body.includes("Data at the Point of Care"),
    "body has 'Log in'": (r) => r.body.includes("Log in"),
  });
}

