/*global console*/ 
/* eslint no-console: "off" */

import { check } from 'k6';
import exec from 'k6/execution'
import { fetchGoldenMacaroon, generateDPCToken } from './generate-dpc-token.js';
import {
  createGroup,
  createOrganization,
  createPractitioner,
  deleteOrganization,
  exportGroup,
  findOrganizationByNpi,
  findGroupByPractitionerNpi,
  createPatients,
  findPatientsByMbi,
  addPatientsToGroup,
  findJobsById
} from './dpc-api-client.js';
import NPIGeneratorCache from './utils/npi-generator.js';
import MBIGeneratorCache from './utils/mbi-generator.js';

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
      'response has id field': res => res.json().id != undefined,
      'id field is not null': res => res.json().id != null
    }
  );
  const checkOutput2 = check(
    org2,
    {
      'response code was 200': res => res.status === 200,
      'response has id field': res => res.json().id != undefined,
      'id field is not null or undefined': res => res.json().id != null
    }
  )

  if (!checkOutput1 || !checkOutput2) {
    exec.test.abort('failed to create organizations on setup')
  }

  // array returned from setup distributes its members starting from the 1 index
  const orgIds = Array();
  orgIds[1] = org1.json().id;

  return { orgIds: orgIds, goldenMacaroon: goldenMacaroon };
}

export function workflow(data) {
  const npiGenerator = npiGeneratorCache.getGenerator(exec.vu.idInInstance);
  const mbiGenerator = mbiGeneratorCache.getGenerator(exec.vu.idInInstance);

  const orgId = data.orgIds[1];
  const token = generateDPCToken(orgId, data.goldenMacaroon);

  // POST practitioner
  const practitionerResponse = createPractitioner(token, npiGenerator.iterate());
  if (practitionerResponse.status != 201) {
    console.error('failed to create practitioner for workflow A');
  }
  // There's only 1 identifier in our synthetic practitioner, so we don't have to search for npi
  const practitionerNpi = practitionerResponse.json().identifier[0].value;
  const practitionerId = practitionerResponse.json().id;

  // POST patients
  const patientResponses = createPatients(token, 14, mbiGenerator);
  const patients = [];
  patientResponses.forEach((res) => {
    if (res.status != 201) {
      console.error('failed to create patient for workflow A');
    } else {
      const json = res.json();
      const patientId = json.id;
      const patientMbi = json.identifier[0].value;
      patients.push({ patientId, patientMbi });
    }
  });

  // POST group for practitioner
  const createGroupResponse = createGroup(token, orgId, practitionerId, practitionerNpi);
  if (createGroupResponse.status != 201) {
    console.error('failed to create group for workflow A');
  }
  const groupId = createGroupResponse.json().id;

  // GET group by practitioner NPI
  const getGroupResponse = findGroupByPractitionerNpi(token, practitionerNpi);
  if (getGroupResponse.status != 200) {
    console.error('failed to get group for workflow B');
  }

  // GET patients by MBI
  const findPatientsResponses = findPatientsByMbi(token, patients.map((patient) => patient.patientMbi));
  findPatientsResponses.forEach((res) => {
    if (res.status != 200) {
      console.error('failed to GET patient.');
    }
  });

  // distribute (PUT) patients into group
  const updateGroupResponses = addPatientsToGroup(token, orgId, groupId, patients.splice(0, 5), practitionerId, practitionerNpi);
  updateGroupResponses.forEach((res) => {
    if (res.status != 200) {
      console.error('failed to add patient to Group.');
    }
  })
  

  // GET group export
  const jobIds = [];
  for (let i = 0; i < 5; i++) {
    const getGroupExportResponse = exportGroup(token, groupId);
    if (getGroupExportResponse.status != 202) {
      console.error('failed to export group for workflow A');
    } else {
      const jobId = getGroupExportResponse.headers['Content-Location'].split('/').pop();
      if (!jobId) {
        console.error('failed to get a location to query the export job');
      } else {
        jobIds.push(jobId);
      }
    }
  }

  // GET job status
  for (let i = 0; i < 2; i++) {
    const jobResponses = findJobsById(token, jobIds);
    jobResponses.forEach((jobResponse) => {
      if (jobResponse.status != 200 && jobResponse.status != 202) {
        console.error('failed to successfully query job in workflow A');
      }
    });
  }
}

export function teardown(data) {
  for (const orgId of data.orgIds) {
    if (orgId) {
      deleteOrganization(orgId, data.goldenMacaroon);
    }
  }
}
