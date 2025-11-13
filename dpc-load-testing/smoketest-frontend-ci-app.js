/*global console*/
/* eslint no-console: "off" */

import http from 'k6/http';
import { check, fail } from 'k6';

function getEnvVar(varName) {
  const value = __ENV[varName];
  if (!value) {
    fail(`Failed to retrieve environment variable: ${varName}`)
  }
  return value
}

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

export default function workflow() {
  if (getEnvVar("ENVIRONMENT") !== "prod" && getEnvVar("ENVIRONMENT") !== "sandbox") {
    // port from src/test/portal_test.yml
    checkLoginPage(getEnvVar("PORTAL_HOST"), ["/portal", "/portal/organizations"], "Sign in");
  }
  if (getEnvVar("ENVIRONMENT") !== "prod") {
    // port from src/test/web_test.yml
    checkLoginPage(getEnvVar("WEB_HOST"), ["/users/sign_in", "/"], "Log in");
    // port from src/test/web_admin_test.yml
    checkLoginPage(getEnvVar("WEB_ADMIN_HOST"), ["/admin/internal/sign_in", "/admin/organizations"], "Log in");
  }
}
