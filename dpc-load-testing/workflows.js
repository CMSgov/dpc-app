/*global console*/ 
/* eslint no-console: "off" */

import { check, fail } from 'k6';
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
  findJobs
} from './dpc-api-client.js';
import NPIGeneratorCache from './utils/npi-generator.js';
import MBIGeneratorCache from './utils/mbi-generator.js';
import { constants } from './constants.js';

const requestCounts = {
  createPatient: 14,
  addPatientsToGroup: 5,
  getGroupExport: 5,
  findJobsById: 2,
}

const npiGeneratorCache = new NPIGeneratorCache();
const mbiGeneratorCache = new MBIGeneratorCache();

// Sets up two test organizations
export function setup() {
  const goldenMacaroon = fetchGoldenMacaroon();
  const orgIds = Array();
  const npiGenerator = npiGeneratorCache.getGenerator(0);
  // array returned from setup distributes its members starting from the 1 index
  for (let i = 1; i <= constants.maxVUs; i++) {
    const npi = npiGenerator.iterate();
    // check if org with npi exists
    const existingOrgResponse = findOrganizationByNpi(npi, goldenMacaroon);
    const checkFindOutput = check(
      existingOrgResponse,
      {
        'response code was 200': res => res.status === 200,
      }
    );

    if (!checkFindOutput) {
      exec.test.abort('failed to check for existing orgs');
    }
    // delete if org exists with npi
    const existingOrgs =  existingOrgResponse.json();
    if ( existingOrgs.total ) {
      for ( const entry of existingOrgs.entry ) {
        deleteOrganization(entry.resource.id, goldenMacaroon);
      }
    }

    const org = createOrganization(npi, `Test Org ${i}`, goldenMacaroon);

    const checkOutput = check(
      org,
      {
        'response code was 200': res => res.status === 200,
        'response has id field': res => res.json().id != undefined,
        'id field is not null': res => res.json().id != null
      }
    );

    if (!checkOutput) {
      exec.test.abort('failed to create organizations on setup')
    }

    orgIds[i] = org.json().id;
  }

  return { orgIds: orgIds, goldenMacaroon: goldenMacaroon };
}

export function workflow(data) {
  const npiGenerator = npiGeneratorCache.getGenerator(exec.vu.idInInstance);
  const mbiGenerator = mbiGeneratorCache.getGenerator(exec.vu.idInInstance);

  const orgId = data.orgIds[exec.vu.idInInstance];
  if (!orgId) {
    fail('error indexing VU ID against orgIds array');
  }

  const token = generateDPCToken(orgId, data.goldenMacaroon);

  // POST practitioner
  const practitionerResponse = createPractitioner(token, npiGenerator.iterate());
  switch (practitionerResponse.status) {
    case 200:   // Already existed, but we can still use it
      console.warn('Attempted to create practitioner with duplicate NPI')
      break;
    case 201:   // Practitioner created
      break;
    case 0:
      console.error('Failed to create practitioner: ' + practitionerResponse.error);
      break;
    default:
      fail('failed to create practitioner, status: ' + practitionerResponse.status);
  }
  // There's only 1 identifier in our synthetic practitioner, so we don't have to search for npi
  const practitionerNpi = practitionerResponse.json().identifier[0].value;
  const practitionerId = practitionerResponse.json().id;

  // POST patients
  const patientResponses = createPatients(token, requestCounts.createPatient, mbiGenerator);
  const patients = [];
  patientResponses.forEach((res) => {
    switch (res.status) {
      case 200: // Already existed, but we can still use it
        console.warn('Attempted to create patient with duplicate mbi');
      case 201: // Patient created
        const json = res.json();
        const patientId = json.id;
        const patientMbi = json.identifier[0].value;
        patients.push({ patientId, patientMbi });
        break;
      case 0:
        console.error('Failed to create patient: ' + res.error);
        break;
      default:
        console.error('failed to create patient, status: ' + res.status);
    }
  });

  if (patients.length === 0) {
    fail('failed to create any patients');
  }

  // POST group for practitioner
  const createGroupResponse = createGroup(token, orgId, practitionerId, practitionerNpi);
  if (createGroupResponse.status != 201) {
    fail('failed to create group');
  }
  const groupId = createGroupResponse.json().id;

  // GET group by practitioner NPI
  const getGroupResponse = findGroupByPractitionerNpi(token, practitionerNpi);
  if (getGroupResponse.status != 200) {
    console.error('failed to get group');
  }

  // GET patients by MBI
  const findPatientsResponses = findPatientsByMbi(token, patients.map((patient) => patient.patientMbi));
  findPatientsResponses.forEach((res) => {
    if (res.status != 200) {
      console.error('failed to GET patient');
    }
  });

  // distribute (PUT) patients into group
  const updateGroupResponses = addPatientsToGroup(token, orgId, groupId, patients.splice(0, requestCounts.addPatientsToGroup), practitionerId, practitionerNpi);
  if (!updateGroupResponses.some(res => res.status === 200)) {
    fail('failed to add any patients to group');
  }
  updateGroupResponses.forEach((res) => {
    if (res.status != 200) {
      console.error('failed to add patient to group');
    }
  })
  

  // GET group export
  const findJobUrls = []
  for (let i = 0; i < requestCounts.getGroupExport; i++) {
    const getGroupExportResponse = exportGroup(token, groupId);
    if (getGroupExportResponse.status != 202) {
      console.error('failed to export group');
    } else {
      let findJobUrl = getGroupExportResponse.headers['Content-Location'];
      if (!findJobUrl) {
        console.error('failed to get a location to query the export job');
      } else {
        if (__ENV.ENVIRONMENT === 'local') {
          findJobUrl = findJobUrl.replace('localhost', 'host.docker.internal');
        }
        findJobUrls.push(findJobUrl);
      }
    }
  }

  // GET job status
  for (let i = 0; i < requestCounts.findJobsById; i++) {
    const jobResponses = findJobs(token, findJobUrls);
    jobResponses.forEach((jobResponse) => {
      if (jobResponse.status != 200 && jobResponse.status != 202) {
        console.error('failed to successfully query job');
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
