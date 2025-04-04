import http from 'k6/http';
import {
  generateOrganizationResourceBody,
  generateProviderResourceBody,
  generatePatientResourceBody,
  generateGroupResourceBody,
  generateProvenanceResourceBody
} from "./resource-request-bodies.js"

const urlRoot = __ENV.ENVIRONMENT == 'local' ? 'http://host.docker.internal:3002/api/v1' : 'https://test.dpc.cms.gov/api/v1';

export function findOrganizationByNpi(npiA, npiB, goldenMacaroon) {
  const res = http.get(`${urlRoot}/Admin/Organization?npis=npi|${npiA},${npiB}`, {
    headers: {
      'Authorization': `Bearer ${goldenMacaroon}`,
      'Content-Type': 'application/fhir+json',
      'Accept': 'application/fhir+json'
    }
  });

  return res;
}


export function createOrganization(npi, name, goldenMacaroon) {
  const body = generateOrganizationResourceBody(npi, name);
  const res = http.post(`${urlRoot}/Organization/$submit`, JSON.stringify(body), {
    headers: {
      'Authorization': `Bearer ${goldenMacaroon}`,
      'Content-Type': 'application/fhir+json',
      'Accept': 'application/fhir+json'
    }
  });

  return res;
}

export function createPractitioner(token, npi) {
  const body = generateProviderResourceBody(npi);
  const res = http.post(`${urlRoot}/Practitioner`, JSON.stringify(body), createHeaderParam(token));

  return res;
}

export function findPractitionerByNpi(token, npi) {
  return http.get(`${urlRoot}/Practitioner?identifier=${npi}`, createHeaderParam(token));
}

export function createPatient(token, mbi) {
  const body = generatePatientResourceBody(mbi);
  const res = http.post(`${urlRoot}/Patient`, JSON.stringify(body), createHeaderParam(token));

  return res;
}

export function findPatientByMbi(token, mbi) {
  return http.get(`${urlRoot}/Patient?identifier=${mbi}`, createHeaderParam(token));
}

export function getOrganization(token) {
  const res = http.get(`${urlRoot}/Organization`, createHeaderParam(token));

  return res;
}

export function deleteOrganization(orgId, goldenMacaroon) {
  const res = http.del(`${urlRoot}/Organization/${orgId}`, null, createHeaderParam(goldenMacaroon));

  return res;
}

export function createGroup(token, orgId, practitionerId, practitionerNpi) {
    const groupBody = generateGroupResourceBody(practitionerNpi);
    const provenanceBody = generateProvenanceResourceBody(orgId, practitionerId);
    const res = http.post(`${urlRoot}/Group`, JSON.stringify(groupBody),
      createHeaderParam(token, {'X-Provenance': JSON.stringify(provenanceBody)})
    );

    return res;
}

export function getGroup(token, groupId) {
    let url;
    if (groupId != undefined) {
        url = `${urlRoot}/Group/${groupId}`;
    } else {
        url = `${urlRoot}/Group`;
    }
    const res = http.get(url, createHeaderParam(token));

    return res;
}

export function findGroupByPractitionerNpi(token, practitionerNpi) {
  return http.get(`${urlRoot}/Group?characteristic-value=attributed-to$${practitionerNpi}`, createHeaderParam(token));
}

export function updateGroup(token, orgId, groupId, patientId, practitionerId, practitionerNpi) {
    const groupBody = generateGroupResourceBody(practitionerNpi, patientId);
    const provenanceBody = generateProvenanceResourceBody(orgId, practitionerId);
    const res = http.put(`${urlRoot}/Group/${groupId}`, JSON.stringify(groupBody),
      createHeaderParam(token, {'X-Provenance': JSON.stringify(provenanceBody)})
    );

    return res;
}

export function exportGroup(token, groupId) {
    const res = http.get(`${urlRoot}/Group/${groupId}/$export`,
      createHeaderParam(token, {'Prefer': 'respond-async'})
    );

      return res;
}

export function findJobById(token, jobId) {
  // Request fails with 406 status when made with Content-Type or Accept header
  return http.get(`${urlRoot}/Jobs/${jobId}`, { 'headers': { 'Authorization': `Bearer ${token}` } });
}

/**
 * Returns a Parameters object with the default headers we use for every request, along with any additional
 * headers passed in.
 * @param {*} orgId
 * @param {*} headers Additional headers that should be included.
 * @returns Headers wrapped in a Parameters object.
 */
function createHeaderParam(token, headers) {
  const defaultHeaders = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/fhir+json',
    'Accept': 'application/fhir+json',
  }

  return {'headers': {...defaultHeaders, ...headers}};
}
