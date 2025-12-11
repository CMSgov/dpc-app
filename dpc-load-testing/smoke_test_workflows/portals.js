/*global console*/
/* eslint no-console: "off" */

import { check } from 'k6';
import exec from 'k6/execution';
import http from 'k6/http';

const portals = {
  'admin': { envs: ['local', 'dev', 'test', 'sandbox'],
             signInPath: 'admin/internal/sign_in',
             protectedPath: 'admin/organizations',
             signInText: 'Log in' },
  'web': { envs: ['local', 'dev', 'test', 'sandbox'],
           signInPath: 'users/sign_in',
           protectedPath: 'organizations/foo/edit',
           signInText: 'Log in' },
  'portal': { envs: ['local', 'dev', 'test'],
              signInPath: 'users/sign_in',
              protectedPath: 'organizations',
              signInText: 'Sign in' },
}

export async function checkPortalsWorkflow(data) {
  const service = Object.keys(portals)[data.idx];
  const config = portals[service];

  if (!config['envs'].includes(__ENV.ENVIRONMENT)) {
    console.log(`${service} not configured for ${__ENV.ENVIRONMENT} environment`);
    return;
  }
  const host = urlRoot(service);
  const signInUrl = `${host}/${config['signInPath']}`;
  const signInResponse = http.get(signInUrl);
  const checkSignIn = check(
    signInResponse,
    {
      'sign in should return 200': res => res.status == '200',
      'sign in should have DPC': res => res.body.includes("Data at the Point of Care"),
      'sign in should have sign in text': res => res.body.includes(config['signInText'])
    }
  );
  if (!checkSignIn){
    console.error(signInResponse.status);
    console.error(signInResponse.body);
    return;
  }

  const protectedPathResponse = http.get(`${host}/${config['protectedPath']}`, { redirects: 0 });
  const checkProtectedPath = check(
    protectedPathResponse,
    {
      'protected path should return 302': res => res.status == '302',
      'protected path has location header': res => res.headers['Location'],
      'protected path location header should be sign in url': res => res.headers['Location'] == signInUrl
    }
  );

  if (!checkProtectedPath){
    console.error(protectedPathResponse.status);
    console.error(protectedPathResponse.headers);
  }
}

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
