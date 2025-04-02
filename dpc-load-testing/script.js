import { check, fail } from 'k6';
import exec from 'k6/execution'
import { fetchGoldenMacaroon, generateDPCToken } from './generate-dpc-token.js';
import {
  createGroup,
  createOrganization,
  createPatient,
  createPractitioner,
  deleteOrganization,
  exportGroup,
  findOrganizationByNpi,
  findPatientByMbi,
  findPractitionerByNpi,
  getGroup,
  findGroupByPractitionerNpi,
  updateGroup,
  findJobById
} from './dpc-api-client.js';
import NPIGeneratorCache from './utils/npi-generator.js';
import MBIGeneratorCache from './utils/mbi-generator.js';

// See https://grafana.com/docs/k6/latest/using-k6/k6-options/reference for
// details on this configuration object.
export const options = {
  scenarios: {
    workflow_a: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: "workflowA"
    },
    workflow_b: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: "workflowB"
    }
  }
};

const npiGeneratorCache = new NPIGeneratorCache();
const mbiGeneratorCache = new MBIGeneratorCache();

// Sets up two test organizations
export function setup() {
  const goldenMacaroon = fetchGoldenMacaroon();
  // Fake NPIs generated online: https://jsfiddle.net/alexdresko/cLNB6
  const existingOrgsResponse = findOrganizationByNpi('2782823019', '8197402604', goldenMacaroon);
  const checkFindOutput = check(
    existingOrgsResponse,
    {
      'response code was 200': res => res.status === 200,
    }
  );
  if (!checkFindOutput) {
    exec.test.abort('failed to check for existing orgs');
  }

  const existingOrgs =  existingOrgsResponse.json();
  if ( existingOrgs.total ) {
    for ( const entry of existingOrgs.entry ) {
      deleteOrganization(entry.resource.id, goldenMacaroon);
    }
  }
  const org1 = createOrganization('2782823019', 'Test Org 1', goldenMacaroon);
  const org2 = createOrganization('8197402604', 'Test Org 2', goldenMacaroon);

  const checkOutput1 = check(
    org1,
    {
      'response code was 200': res => res.status === 200,
      'response has id field': res => res.json().hasOwnProperty('id'),
      'id field is not null or undefined': res => res.json().id != null && res.json().id != undefined
    }
  );
  const checkOutput2 = check(
    org2,
    {
      'response code was 200': res => res.status === 200,
      'response has id field': res => res.json().hasOwnProperty('id'),
      'id field is not null or undefined': res => res.json().id != null && res.json().id != undefined
    }
  )

  if (!checkOutput1 || !checkOutput2) {
    exec.test.abort('failed to create organizations on setup')
  }

  // array returned from setup distributes its members starting from the 1 index
  const orgIds = Array();
  orgIds[1] = org1.json().id;
  orgIds[2] = org2.json().id;

  return { orgIds: orgIds, goldenMacaroon: goldenMacaroon };
}

export function workflowA(data) {
  const npiGenerator = npiGeneratorCache.getGenerator(__VU);
  const mbiGenerator = mbiGeneratorCache.getGenerator(__VU);

  const orgId = data.orgIds[exec.vu.idInInstance];
  const token = generateDPCToken(orgId, data.goldenMacaroon);

  // POST practitioner
  const practitionerResponse = createPractitioner(token, npiGenerator.iterate());
  if (practitionerResponse.status != 201) {
    fail('failed to create practitioner for workflow A');
  }
  // There's only 1 identifier in our synthetic practitioner, so we don't have to search for npi
  const practitionerNpi = practitionerResponse.json().identifier[0].value;
  const practitionerId = practitionerResponse.json().id;

  // POST patient
  const patientResponse = createPatient(token, mbiGenerator.iterate());
  if (patientResponse.status != 201) {
    fail('failed to create patient for workflow A');
  }
  const patientId = patientResponse.json().id;

  // POST group
  const createGroupResponse = createGroup(token, orgId, practitionerId, practitionerNpi);
  if (createGroupResponse.status != 201) {
    fail('failed to create group for workflow A');
  }
  const groupId = createGroupResponse.json().id;

  // GET all groups
  const getGroupsResponse = getGroup(token);
  if (getGroupsResponse.status != 200) {
    fail('failed to get groups for workflow A');
  }
  // There should only be one group returned
  const foundGroupId = getGroupsResponse.json().entry[0].resource.id;
  if (foundGroupId != groupId) {
    fail("failed to find created group for workflow A");
  }

  // PUT patient in group
  const updateGroupResponse = updateGroup(token, orgId, groupId, patientId, practitionerId, practitionerNpi);
  if (updateGroupResponse.status != 200) {
    fail('failed to update group for workflow A');
  }

  // GET specific group
  const getGroupResponse = getGroup(token, groupId);
  if (getGroupResponse.status != 200) {
    fail('failed to read group for workflow A');
  }
  // Should only be a reference to one patient, in the format "Patient/id"
  const addedPatientId = getGroupResponse.json().member[0].entity.reference.replace("Patient/", "");
  if (addedPatientId != patientId) {
    fail('patient not found in group for workflow A');
  }

  // GET group export
  const getGroupExportResponse = exportGroup(token, groupId);
  if (getGroupExportResponse.status != 202) {
    fail('failed to export group for workflow A');
  }

  const jobId = getGroupExportResponse.headers['Content-Location'].split('/').pop();
  if (!jobId) {
    fail('failed to get a location to query the export job in workflow B');
  }
  const jobResponse = findJobById(token, jobId);
  if (jobResponse.status != 200 && jobResponse.status != 202) {
    fail('failed to successfully query job in workflow A');
  }
  
  // TODO: we'll want to continue to check the status of active jobs while they exist. Put them into an array and, on each iteration, query them until done.
}

export function workflowB(data) {
  const npiGenerator = npiGeneratorCache.getGenerator(__VU);
  const mbiGenerator = mbiGeneratorCache.getGenerator(__VU);

  const orgId = data.orgIds[exec.vu.idInInstance];
  const token = generateDPCToken(orgId, data.goldenMacaroon);
  
  // POST practitioner
  const postPractitionerResponse = createPractitioner(token, npiGenerator.iterate());
  if (postPractitionerResponse.status != 201) {
    fail('failed to create practitioner for workflow B');
  }
  // There's only 1 identifier in our synthetic practitioner, so we don't have to search for npi
  const practitionerNpi = postPractitionerResponse.json().identifier[0].value;
  const practitionerId = postPractitionerResponse.json().id;

  // POST patient
  let postPatientResponse = createPatient(token, mbiGenerator.iterate());
  if (postPatientResponse.status != 201) {
    fail('failed to create patient for workflow B');
  }
  const patientId = postPatientResponse.json().id;
  const patientMbi = postPatientResponse.json().identifier[0].value;

  // POST group
  const createGroupResponse = createGroup(token, orgId, practitionerId, practitionerNpi);
  if (createGroupResponse.status != 201) {
    fail('failed to create group for workflow B');
  }
  let groupId = createGroupResponse.json().id;

  // GET practitioner
  const getPractitionerResponse = findPractitionerByNpi(token, practitionerNpi);
  if (getPractitionerResponse.status != 200) {
    fail('failed to get practioner for workflow B');
  }
  const practitionerResource = getPractitionerResponse.json();

  // GET patient
  const getPatientResponse = findPatientByMbi(token, patientMbi);
  if (getPatientResponse.status != 200) {
    fail('failed to get patient for workflow B');
  }
  const patientResource = getPatientResponse.json();

  // GET group by practitioner NPI
  const getGroupResponse = findGroupByPractitionerNpi(token, practitionerNpi);
  if (getGroupResponse.status != 200) {
    fail('failed to get group for workflow B');
  }

  // PUT patient in group
  const updateGroupResponse = updateGroup(token, orgId, groupId, patientId, practitionerId, practitionerNpi);
  if (updateGroupResponse.status != 200) {
    fail('failed to update group for workflow B');
  }

  // GET group export
  const getGroupExportResponse = exportGroup(token, groupId);
  if (getGroupExportResponse.status != 202) {
    fail('failed to export group for workflow B');
  }
  
  const jobId = getGroupExportResponse.headers['Content-Location'].split('/').pop();
  if (!jobId) {
    fail('failed to get a location to query the export job in workflow B');
  }
  const jobResponse = findJobById(token, jobId);
  if (jobResponse.status != 200 && jobResponse.status != 202) {
    fail('failed to successfully query job in workflow B');
  }
  
  // TODO: we'll want to continue to check the status of active jobs while they exist. Put them into an array and, on each iteration, query them until done.
}

export function teardown(data) {
  for (const orgId of data.orgIds) {
    if (orgId) {
      deleteOrganization(orgId, data.goldenMacaroon);
    }
  }
}
