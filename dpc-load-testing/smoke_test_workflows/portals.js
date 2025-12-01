/*global console*/
/* eslint no-console: "off" */

import { check } from 'k6';
import exec from 'k6/execution';
import http from 'k6/http';

const adminEnvs = ['local', 'dev', 'test', 'sandbox'];
const webEnvs = ['local', 'dev', 'test', 'sandbox'];
const portalEnvs = ['local', 'dev', 'test'];


function urlRoot(service) {
  if (__ENV.ENVIRONMENT != 'local') {
    return `https://${__ENV.ENVIRONMENT}.dpc.cms.gov`;
  } else if (service == 'portal') {
    return 'http://host.docker.internal:3100';
  } else if (service == 'admin') {
    return 'http://host.docker.internal:3000';
  } else if (service == 'web') {
    return 'http://host.docker.internal:3900';
  } else {
    console.error(`Request for host from non-existent service ${service}`);
    exec.test.fail();
  }
}
export async function checkPortalsWorkflow(data) {
  switch (data.idx) {
  case 0:
    checkPortal();
    break;
  case 1:
    checkAdmin();
    break;
  case 2:
    checkWeb();
    break;
  default:
    console.log(`invalid vu index ${data.idx}`);
    exec.test.fail();
  }
}

function checkPortal(){
  if (!portalEnvs.includes(__ENV.ENVIRONMENT)) {
    console.log(`Portal not configured for ${__ENV.ENVIRONMENT} environment`);
    return;
  }
  const host = urlRoot('portal');
  const signInUrl = `${host}/portal/users/sign_in`;
  const signInResponse = http.get(signInUrl);
  const checkSignIn = check(
    signInResponse,
    {
      'portal sign in should be 200': res => res.status == '200',
      'portal sign in should have DPC':  res => res.body.includes("Data at the Point of Care"),
      'portal sign in should have sign in': res => res.body.includes("Sign in")
    }
  );
  if (!checkSignIn){
    console.error(signInResponse.status);
    console.error(signInResponse.body);
    return;
  }
  const organizationsResponse = http.get(`${host}/portal/organizations`);
  const checkOrganizations = check(
    organizationsResponse,
    {
      'portal organizations should be 200': res => res.status == '200',
      'portal organizations should have DPC':  res => res.body.includes("Data at the Point of Care"),
      'portal organizations should have sign in': res => res.body.includes("Sign in"),
      'portal organizations response url should be sign in': res => res.url == signInUrl,
    }

  );
  if (!checkOrganizations){
    console.error(organizationsResponse.status);
    console.error(organizationsResponse.body);
    console.error(organizationsResponse.url);
  }
}

function checkAdmin(){
  if (!adminEnvs.includes(__ENV.ENVIRONMENT)) {
    console.log(`Admin not configured for ${__ENV.ENVIRONMENT} environment`);
    return;
  }
  const host = urlRoot('admin');
  const signInUrl = `${host}/admin/internal/sign_in`;
  const signInResponse = http.get(signInUrl);
  const checkSignIn = check(
    signInResponse,
    {
      'admin sign in should be 200': res => res.status == '200',
      'admin sign in should have DPC':  res => res.body.includes("Data at the Point of Care"),
      'admin sign in should have log in': res => res.body.includes("Log in")
    }
  );
  if (!checkSignIn){
    console.error(signInResponse.status);
    console.error(signInResponse.body);
    return;
  }
  const organizationsResponse = http.get(`${host}/admin/organizations`);
  const checkOrganizations = check(
    organizationsResponse,
    {
      'admin organizations should be 200': res => res.status == '200',
      'admin organizations should have DPC':  res => res.body.includes("Data at the Point of Care"),
      'admin organizations should have log in': res => res.body.includes("Log in"),
      'admin organizations response url should be sign in': res => res.url == signInUrl,
    }

  );
  if (!checkOrganizations){
    console.error(organizationsResponse.status);
    console.error(organizationsResponse.body);
    console.error(organizationsResponse.url);
  }
}

function checkWeb(){
  if (!webEnvs.includes(__ENV.ENVIRONMENT)) {
    console.log(`Web not configured for ${__ENV.ENVIRONMENT} environment`);
    return;
  }
  const host = urlRoot('web');
  const signInUrl = `${host}/users/sign_in`;
  const signInResponse = http.get(signInUrl);
  const checkSignIn = check(
    signInResponse,
    {
      'web sign in should be 200': res => res.status == '200',
      'web sign in should have DPC':  res => res.body.includes("Data at the Point of Care"),
      'web sign in should have log in': res => res.body.includes("Log in")
    }
  );
  if (!checkSignIn) {
    console.error(signInResponse.status);
    console.error(signInResponse.body);
    return;
  }

  const organizationsResponse = http.get(`${host}/organizations/foo/edit`);
  const checkOrganizations = check(
    organizationsResponse,
    {
      'web organizations should be 200': res => res.status == '200',
      'web organizations should have DPC':  res => res.body.includes("Data at the Point of Care"),
      'web organizations should have log in': res => res.body.includes("Log in"),
      'web organizations response url should be sign in': res => res.url == signInUrl,
    }
  );
  if (!checkOrganizations) {
    console.error(organizationsResponse.status);
    console.error(organizationsResponse.body);
    console.error(organizationsResponse.url);
  }
}
