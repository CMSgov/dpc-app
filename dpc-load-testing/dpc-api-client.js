import http from 'k6/http';
import { generateOrganizationResourceBody } from "./resource-request-bodies.js"

const urlRoot = __ENV.ENVIRONMENT == 'local' ? 'http://host.docker.internal:3002/v1' : 'https://test.dpc.cms.gov/api/v1';
const goldenMacaroon = __ENV.ENVIRONMENT == 'local' ? 'INSERT_MACAROON_HERE' : __ENV.GOLDEN_MACAROON;

export function createOrganization(npi, name) {
  const orgResourceBody = generateOrganizationResourceBody(npi, name);
  const res = http.post(`${urlRoot}/Organization/$submit`, JSON.stringify(orgResourceBody), {
    headers: {
      'Authorization': `Bearer ${goldenMacaroon}`,
      'Content-Type': 'application/fhir+json',
      'Accept': 'application/fhir+json'
    }
  });

  return res;
}
