const envUrl = new Map([
  ['local', 'http://localhost:3002'],
  ['docker', 'http://host.docker.internal:3002'],
  ['dev', 'https://dev.dpc.cms.gov/api'],
  ['test', 'https://test.dpc.cms.gov/api'],
  ['sandbox', 'https://sandbox.dpc.cms.gov/api'],
  ['prod', 'https://prod.dpc.cms.gov/api']
]);

export function getBaseUrl() {
  let url = envUrl.has(`${__ENV.env}`) ? envUrl.get(`${__ENV.env}`) : envUrl.get('test');
  console.log(`Using base URL ${url}`);
  return url;
}
