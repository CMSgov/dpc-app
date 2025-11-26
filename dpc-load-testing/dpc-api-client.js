import http from 'k6/http';

import {
  generateBundle,
  generateOrganizationResourceBody,
  generateProviderResourceBody,
  generatePatientResourceBody,
  generateGroupResourceBody,
  generateProvenanceResourceBody
} from "./resource-request-bodies.js"

export const urlRoot = __ENV.ENVIRONMENT == 'local' ? 'http://host.docker.internal:3002/api/v1' : `https://${__ENV.ENVIRONMENT}.dpc.cms.gov/api/v1`;

export function findOrganizationByNpi(npi, goldenMacaroon) {
  const res = http.get(`${urlRoot}/Admin/Organization?npis=npi|${npi}`, {
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

export function createSmokeTestOrganization(npi, orgId, goldenMacaroon) {
  const body = generateOrganizationResourceBody(npi, `SmokeTest ${orgId}`, orgId);
  const res = http.post(`${urlRoot}/Organization/$submit`, JSON.stringify(body), {
    headers: {
      'Authorization': `Bearer ${goldenMacaroon}`,
      'Content-Type': 'application/fhir+json',
      'Accept': 'application/fhir+json'
    }
  });

  return res;
}

export function getOrganizationById(token, orgId) {
  const headers = createHeaderParam(token);
  const res = http.get(`${urlRoot}/Organization/${orgId}`, headers);

  return res;
}

export function getOrganizationByAccessToken(token) {
  const headers = createHeaderParam(token);
  const res = http.get(`${urlRoot}/Organization`, headers);

  return res;
}

export function updateOrganization(token, organization, contentTypeHeader=null) {
  const headers = createHeaderParam(token);
  if (contentTypeHeader) {
    headers['headers']['Content-Type'] = contentTypeHeader;
  }
  const orgId = organization.id;
  const res = http.put(`${urlRoot}/Organization/${orgId}`, JSON.stringify(organization), headers);

  return res;
}

export function createPractitioner(token, npi) {
  const body = generateProviderResourceBody(npi);
  const res = http.post(`${urlRoot}/Practitioner`, JSON.stringify(body), createHeaderParam(token));

  return res;
}

export function createPractitioners(token, npi) {
  const body = generateBundle([{"resource": generateProviderResourceBody(npi)}]);
  const res = http.post(`${urlRoot}/Practitioner/$submit`, JSON.stringify(body), createHeaderParam(token));

  return res;
}

export function createPractitionersRawData(token, rawData) {
  const res = http.post(`${urlRoot}/Practitioner/$submit`, rawData, createHeaderParam(token));

  return res;
}

export function deletePractitioner(token, practitionerId) {
  return http.del(`${urlRoot}/Practitioner/${practitionerId}`, null, createHeaderParam(token));
}

export function findPractitionerByNpi(token, npi) {
  return http.get(`${urlRoot}/Practitioner?identifier=${npi}`, createHeaderParam(token));
}

export function createPatient(token, mbi) {
  const body = generatePatientResourceBody(mbi);
  const res = http.post(`${urlRoot}/Patient`, JSON.stringify(body), createHeaderParam(token));

  return res;
}

export function createPatients(token, number, mbiGenerator) {
  const batchRequests = [];
  for (let i = 0; i < number; i++) {
    const mbi = mbiGenerator.iterate();
    const body = generatePatientResourceBody(mbi);
    const request = {
      method: 'POST',
      url: `${urlRoot}/Patient`,
      body: JSON.stringify(body),
      params: createHeaderParam(token)
    };

    batchRequests.push(request);
  }

  const res = http.batch(batchRequests);
  return res;
}

export function createPatientsBatch(token, mbis) {
  const entries = [];
  mbis.forEach((mbi) => entries.push({'resource': generatePatientResourceBody(mbi)}));
  const body = generateBundle(entries);
  const res = http.post(`${urlRoot}/Patient/$submit`, JSON.stringify(body), createHeaderParam(token));

  return res;
}

export function createPatientsRawData(token, rawData) {
  const res = http.post(`${urlRoot}/Patient/$submit`, rawData, createHeaderParam(token));

  return res;
}

export function deletePatient(token, patientId) {
  return http.del(`${urlRoot}/Patient/${patientId}`, null, createHeaderParam(token));
}

export function findPatientByMbi(token, mbi) {
  return http.get(`${urlRoot}/Patient?identifier=${mbi}`, createHeaderParam(token));
}

export function findPatientsByMbi(token, mbis) {
  const batchRequests = mbis.map((mbi) => {
    return {
      method: 'GET',
      url: `${urlRoot}/Patient?identifier=${mbi}`,
      params: createHeaderParam(token)
    }
  });

  const res = http.batch(batchRequests);
  return res;
}

export function patientEverything(token, orgId, practitionerId, patientId) {
  const provenanceBody = generateProvenanceResourceBody(orgId, practitionerId);
  return http.get(`${urlRoot}/Patient/${patientId}/$everything`,
		  createHeaderParam(token, {'X-Provenance': JSON.stringify(provenanceBody)})
		 );
}

export function removePatientFromGroup(token, orgId, practitionerId, practitionerNpi, groupId, patientId) {
  const groupBody = generateGroupResourceBody(practitionerNpi);
  groupBody['member'] = [{'entity': {'reference': `Patient/${patientId}`}}];
  const provenanceBody = generateProvenanceResourceBody(orgId, practitionerId);
  const res = http.post(`${urlRoot}/Group/${groupId}/$remove`, JSON.stringify(groupBody),
			createHeaderParam(token, {'X-Provenance': JSON.stringify(provenanceBody)})
		       );

  return res;
}

export function getOrganization(token, gzipped=null) {
  const headers = createHeaderParam(token);
  if (gzipped) {
    headers['headers']['Accept-Encoding'] = 'gzip';
  }
  const res = http.get(`${urlRoot}/Organization`, headers);

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

export function createGroupWithPatients(token, orgId, practitionerId, practitionerNpi, patients) {
  const groupBody = generateGroupResourceBody(practitionerNpi);
  const members = [];
  patients.forEach((patient) => members.push({'entity': {'reference': `Patient/${patient}`}}));
  groupBody['member'] = members;
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

export function addPatientsToGroup(token, orgId, groupId, patients, practitionerId, practitionerNpi) {
  const batchRequests = patients.map((patient) => {
    const groupBody = generateGroupResourceBody(practitionerNpi, patient.patientId);
    const provenanceBody = generateProvenanceResourceBody(orgId, practitionerId);
    return {
      method: 'PUT',
      url: `${urlRoot}/Group/${groupId}`,
      body: JSON.stringify(groupBody),
      params: createHeaderParam(token, {'X-Provenance': JSON.stringify(provenanceBody)})
    };
  });

  const res = http.batch(batchRequests);
  return res;
}

export function exportGroup(token, groupId, getParams='') {
    const res = http.get(`${urlRoot}/Group/${groupId}/$export?${getParams}`,
      createHeaderParam(token, {'Prefer': 'respond-async'})
    );

    return res;
}

export function findJobById(token, jobId) {
  // Request fails with 406 status when made with Content-Type or Accept header
  return http.get(`${urlRoot}/Jobs/${jobId}`, { 'headers': { 'Authorization': `Bearer ${token}` } });
}

export function findJobs(token, urls) {
  const batchRequests = urls.map((url) => {
    return {
      method: 'GET',
      url: url,
      params: { 'headers': { 'Authorization': `Bearer ${token}` } }
    }
  });

  const res = http.batch(batchRequests);
  return res;
}

export function createClientToken(token, label) {
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Authorization': `Bearer ${token}`
  };
  const res = http.post(`${urlRoot}/Token`, `{ "label": "${label}" }`, { 'headers': headers });
  return res;
}

export function createPublicKey(token, label, key, signature) {
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Authorization': `Bearer ${token}`
  };
  const keyData = {
    key: key,
    signature: signature
  }
  const res = http.post(`${urlRoot}/Key?label=${label}`, JSON.stringify(keyData), { 'headers': headers });
  return res
}

export function validateJwt(jwt) {
  const headers = {'Accept': 'application/json', 'Content-Type': 'text/plain' };

  const res = http.post(`${urlRoot}/Token/validate`, jwt, { 'headers': headers });
  return res;
}

export function retrieveAccessToken(jwt) {
  const headers = {'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json'};
  const payload = { 'grant_type': 'client_credentials',
                    'scope': 'system/*.*',
                    'client_assertion_type': 'urn:ietf:params:oauth:client-assertion-type:jwt-bearer',
                    'client_assertion': jwt };
  const res = http.post(`${urlRoot}/Token/auth`, payload, { 'headers': headers });
  return res;
}

export function authorizedGet(token, url, headers = {}) {
  headers['Authorization'] = `Bearer ${token}`;
  return http.get(url.replace('localhost', 'host.docker.internal'),
		  { 'headers': headers });
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
