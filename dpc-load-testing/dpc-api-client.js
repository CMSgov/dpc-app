import http from 'k6/http';
import { 
  generateOrganizationResourceBody,
  generateProviderResourceBody,
  generatePatientResourceBody
} from "./resource-request-bodies.js"

const urlRoot = __ENV.ENVIRONMENT == 'local' ? 'http://host.docker.internal:3002/v1' : 'https://test.dpc.cms.gov/api/v1';

export function createOrganization(npi, name) {
  const body = generateOrganizationResourceBody(npi, name);
  const res = http.post(`${urlRoot}/Organization/$submit`, JSON.stringify(body), {
    headers: {
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
      'Organization': orgId,
      'Content-Type': 'application/fhir+json',
      'Accept': 'application/fhir+json'
    }
  });

  return res;
}

export function createPatient(mbi, orgId) {
  const body = generatePatientResourceBody(mbi);
  const res = http.post(`${urlRoot}/Patient`, JSON.stringify(body), {
    headers: {
      'Organization': orgId,
      'Content-Type': 'application/fhir+json',
      'Accept': 'application/fhir+json'
    }
  });

  return res;
}

export function getOrganization(id) {
  const res = http.get(`${urlRoot}/Organization`, {
    headers: {
      'Organization': id,
      'Content-Type': 'application/fhir+json',
      'Accept': 'application/fhir+json'
    }
  });

  return res;
}

export function deleteOrganization(id) {
  const res = http.del(`${urlRoot}/Organization/${id}`, null, {
    headers: {
      'Content-Type': 'application/fhir+json',
      'Accept': 'application/fhir+json'
    }
  });

  return res;
}
