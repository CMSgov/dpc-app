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


function checkLoginPage(baseUrl, paths) {
  if (!baseUrl) {
    throw new Error(`${baseUrl} environment variable is not set`)
  }

  paths.forEach(path => {
    const fullUrl = baseUrl + path;
    const res = http.get(fullUrl);
    console.log("res.status: ", res.status);

    check(res, {
      "is status 200": (r) => r.status === 200,
      "verify homepage text": (r) => r.body.includes("Data at the Point of Care"),
      "verify login text": (r) => r.body.includes("Log in")
    });
  })
}

export function workflow() {
  checkLoginPage(__ENV.PORTAL_HOST, ["/portal", "/portal/organizations"]);
  checkLoginPage(__ENV.WEB_HOST, ["/users/sign_in", "/"]);
  checkLoginPage(__ENV.WEB_ADMIN_HOST, ["/admin/internal/sign_in", "/admin/organizations"]);
}

