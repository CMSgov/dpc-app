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

function checkDPCPortal() {
  const baseUrl = __ENV.PORTAL_HOST;
  if (!baseUrl) {
    throw new Error("PORTAL_HOST environment variable is not set");
  }

  const portalUrl = `${baseUrl}/portal`;
  const portalResponse = http.get(portalUrl);
  check(portalResponse, {
    "is status 200": (r) => r.status === 200,
    "verify homepage text": (r) => r.body.includes("Data at the Point of Care"),
    "verify login text": (r) => r.body.includes("Log in")
  });

  const portalOrganizationsUrl = `${baseUrl}/portal/organizations`;
  const portalOrganizationsResponse = http.get(portalOrganizationsUrl);
  check(portalOrganizationsResponse, {
    "is status 200": (r) => r.status === 200,
    "verify homepage text": (r) => r.body.includes("Data at the Point of Care"),
    "verify login text": (r) => r.body.includes("Log in")
  });
}

function checkDPCWeb() {
  const baseUrl = __ENV.WEB_HOST;
  if (!baseUrl) {
    throw new Error("WEB_HOST environment variable is not set");
  }

  const url = `${baseUrl}/users/sign_in`;
  const res = http.get(url);

  check(res, {
    "is status 200": (r) => r.status === 200,
    "verify homepage text": (r) => r.body.includes("Data at the Point of Care"),
    "verify login text": (r) => r.body.includes("Log in")
  });

  const rootUrl = `${baseUrl}/`;
  const rootRes = http.get(rootUrl);

  check(rootRes, {
    "is status 200": (r) => r.status === 200,
    "verify homepage text": (r) => r.body.includes("Data at the Point of Care"),
    "verify login text": (r) => r.body.includes("Log in")
  });
}

function checkDPCWebAdmin() {
  const baseUrl = __ENV.WEB_ADMIN_HOST;
  if (!baseUrl) {
    throw new Error("WEB_ADMIN_HOST environment variable is not set");
  }

  const adminUrl = `${baseUrl}/admin/internal/sign_in`;
  const adminResponse = http.get(adminUrl);
  check(adminResponse, {
    "is status 200": (r) => r.status === 200,
    "verify homepage text": (r) => r.body.includes("Data at the Point of Care"),
    "verify login text": (r) => r.body.includes("Log in")
  });

  const adminOrganizationsUrl = `${baseUrl}/admin/organizations`;
  const adminOrganizationsResponse = http.get(adminOrganizationsUrl);
  check(adminOrganizationsResponse, {
    "is status 200": (r) => r.status === 200,
    "verify homepage text": (r) => r.body.includes("Data at the Point of Care"),
    "verify login text": (r) => r.body.includes("Log in")
  });
}

export function workflow() {
  checkDPCPortal();
  checkDPCWeb();
  checkDPCWebAdmin();
}

