import http from 'k6/http';
import { 
  generateOrganizationResourceBody,
  generateProviderResourceBody,
  generatePatientResourceBody
} from "./resource-request-bodies.js"
import tokenCache from './generate-dpc-token.js';

const urlRoot = __ENV.ENVIRONMENT == 'local' ? 'http://host.docker.internal:3002/v1' : 'https://test.dpc.cms.gov/api/v1';

export function createOrganization(npi, name) {
  const body = generateOrganizationResourceBody(npi, name);
  const res = http.post(`${urlRoot}/Organization/$submit`, JSON.stringify(body), {
    headers: {
      'Authorization': `Bearer ${tokenCache.goldenMacaroon}`,
      'Content-Type': 'application/fhir+json',
      'Accept': 'application/fhir+json'
    }
  });

  return res;
}

export function createProvider(npi, orgId) {
  const body = generateProviderResourceBody(npi);
  const res = http.post(`${urlRoot}/Practitioner`, JSON.stringify(body), {
    headers: {
      'Authorization': `Bearer ${tokenCache.getToken(orgId)}`,
      'Content-Type': 'application/fhir+json',
      'Accept': 'application/fhir+json',
      'Organization': orgId     // Required if we're not using auth
    }
  });

  return res;
}

export function createPatient(mbi) {
  const body = generatePatientResourceBody(mbi);
  const res = http.post(`${urlRoot}/Patient`, JSON.stringify(body), {
    headers: {
      'Authorization': `Bearer ${tokenCache.token}`,
      'Content-Type': 'application/fhir+json',
      'Accept': 'application/fhir+json'
    }
  });

  return res;
}

export function getOrganization(id) {
  const res = http.get(`${urlRoot}/Organization/${id}`, {
    headers: {
      'Authorization': `Bearer ${tokenCache.token}`,
      'Content-Type': 'application/fhir+json',
      'Accept': 'application/fhir+json'
    }
  });

  return res;
}

export function deleteOrganization(id) {
  const res = http.del(`${urlRoot}/Organization/${id}`, null, {
    headers: {
      'Authorization': `Bearer ${tokenCache.goldenMacaroon}`,
      'Content-Type': 'application/fhir+json',
      'Accept': 'application/fhir+json'
    }
  });

  return res;
}
