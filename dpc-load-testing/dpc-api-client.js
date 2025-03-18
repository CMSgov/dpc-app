import http from 'k6/http';
import { 
  generateOrganizationResourceBody,
  generateProviderResourceBody,
  generatePatientResourceBody,
  generateGroupResourceBody,
  generateProvenanceResourceBody
} from "./resource-request-bodies.js"
import tokenCache from './generate-dpc-token.js';

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
  const res = http.post(`${urlRoot}/Practitioner`, JSON.stringify(body), createHeaderParam(orgId));

  return res;
}

export function createPatient(mbi, orgId) {
  const body = generatePatientResourceBody(mbi);
  const res = http.post(`${urlRoot}/Patient`, JSON.stringify(body), createHeaderParam(orgId));

  return res;
}

export function getOrganization(orgId) {
  const res = http.get(`${urlRoot}/Organization/${orgId}`, createHeaderParam(orgId));

  return res;
}

export function deleteOrganization(orgId) {
  const res = http.del(`${urlRoot}/Organization/${orgId}`, null, createHeaderParam(orgId));

  return res;
}

export function createGroup(orgId, practitionerId, practitionerNpi) {
    const groupBody = generateGroupResourceBody(practitionerNpi);
    const provenanceBody = generateProvenanceResourceBody(orgId, practitionerId);
    const res = http.post(`${urlRoot}/Group`, JSON.stringify(groupBody), 
      createHeaderParam(orgId, {'X-Provenance': JSON.stringify(provenanceBody)})
    );

    return res;
}

export function getGroup(orgId, groupId) {
    if (groupId != undefined) {
        var url = `${urlRoot}/Group/${groupId}`;
    } else {
        var url = `${urlRoot}/Group`;
    }
    const res = http.get(url, createHeaderParam(orgId));
    
    return res;
}

export function updateGroup(orgId, groupId, patientId, practitionerId, practitionerNpi) {
    const groupBody = generateGroupResourceBody(practitionerNpi, patientId);
    const provenanceBody = generateProvenanceResourceBody(orgId, practitionerId);
    const res = http.put(`${urlRoot}/Group/${groupId}`, JSON.stringify(groupBody), 
      createHeaderParam(orgId, {'X-Provenance': JSON.stringify(provenanceBody)})
    );

    return res;
}

export function exportGroup(orgId, groupId) {
    const res = http.get(`${urlRoot}/Group/${groupId}/$export`, 
      createHeaderParam(orgId, {'Prefer': 'respond-async'})
    );
    
      return res;
}

/**
 * Returns a parameters object with the default headers we use for every request, along with any additional
 * headers passed in.
 * @param {*} orgId 
 * @param {*} headers Additional headers that should be included.
 * @returns Headers wrapped in a Parameters object.
 */
function createHeaderParam(orgId, headers) {
  const defaultHeaders = {
    'Content-Type': 'application/fhir+json',
    'Accept': 'application/fhir+json',
    'Organization': orgId   // We need to specify the org for the static auth filter with auth disabled in dpc-api
  }

  return {'headers': Object.assign({}, defaultHeaders, headers)};
}
