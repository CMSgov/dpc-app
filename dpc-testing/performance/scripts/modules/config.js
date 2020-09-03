const envUrl = new Map([
  ['local', 'http://localhost:3002'],
  ['docker', 'http://host.docker.internal:3002'],
  ['dev', 'https://dev.dpc.cms.gov'],
  ['test', 'https://test.dpc.cms.gov'],
  ['sandbox', 'https://sandbox.dpc.cms.gov'],
  ['prod', 'https://prod.dpc.cms.gov']
]);

export function getBaseUrl() {
  let url = envUrl.has(`${__ENV.env}`) ? envUrl.get(`${__ENV.env}`) : envUrl.get('test');
  console.log(`Using base URL ${url}`);
  return url;
}
