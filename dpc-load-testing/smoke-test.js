/*global console*/
/* eslint no-console: "off" */

import { check, sleep } from 'k6';
import exec from 'k6/execution'
import { fetchGoldenMacaroon, generateDPCToken } from './generate-dpc-token.js';
import {
  authorizedGet,
  createGroupWithPatients,
  createPatientsFile,
  createPractitionersFile,
  deleteOrganization,
  exportGroup,
  findGroupByPractitionerNpi,
  getOrganizationById,
  createSmokeTestOrganization,
  findOrganizationByNpi,
} from './dpc-api-client.js';

import NPIGeneratorCache from './utils/npi-generator.js';

const npiGeneratorCache = new NPIGeneratorCache();

export const options = {
  thresholds: {
    checks: ['rate===1'],
  },
  scenarios: {
    bulkExportWorkflow: {
      executor: 'per-vu-iterations',
      vus: 3,
      iterations: 1,
      exec: "bulkExportWorkflow"
    }
  }
};

const EXPORT_POLL_INTERVAL_SEC = __ENV.ENVIRONMENT == 'local' ? 1 : 20;
const JOB_OUPUT_ERROR_LENGTH = __ENV.ENVIRONMENT == 'local' ? 1 : 0;
const practitionerBundle =  __ENV.ENVIRONMENT == 'prod' ? open('./resources/prod_provider_bundle.json') : open('./resources/provider_bundle.json');
const patientBundle =  __ENV.ENVIRONMENT == 'prod' ? open('./resources/prod_patient_bundle-dpr.json') : open('./resources/patient_bundle-dpr.json');
const associationsFile =  __ENV.ENVIRONMENT == 'prod' ? open('./resources/prod_test_associations.csv') : open('./resources/test_associations-dpr.csv');
const practitionerCount =  __ENV.ENVIRONMENT == 'prod' ? 2 : 4;

// Sets up two test organizations
export function setup() {
  const goldenMacaroon = fetchGoldenMacaroon();
  const orgIds = [
    '0ab352f1-2bf1-44c4-aa7a-3004a1ffef12',
    '69c0d4d4-9c07-4fa8-9053-e10fb1608b48',
    'c7f5247b-4c41-478c-84eb-a6e801bdb145'
  ];
  const tokens = Array();
  const npiGenerator = npiGeneratorCache.getGenerator(0);
  // array returned from setup distributes its members starting from the 1 index
  for (let i = 0; i < orgIds.length; i++) {
    // delete smoke test org if present
    const orgId = orgIds[i];
    const token = generateDPCToken(orgId, goldenMacaroon);
    tokens[i] = token;
    const existingOrgResponse = getOrganizationById(token, orgId);

    const checkGetOrgOutput = check(
      existingOrgResponse,
      {
        'find org by id code 200 or 404': res => res.status == 404 || res.status == 200,
      }
    );

    if (!checkGetOrgOutput) {
      console.error(existingOrgResponse.body);
      exec.test.abort('failed find org by id');
    }

    if (existingOrgResponse == 200) {
      deleteOrganization(orgId, goldenMacaroon);
    }
    let npi;
    let count = 0;
    while(count < 200) {
      count += 1;
      const npiToCheck = npiGenerator.iterate();
      // check if org with npi exists
      const existingNpiResponse = findOrganizationByNpi(npiToCheck, goldenMacaroon);

      const checkFindOutput = check(
        existingNpiResponse,
        {
          'find org by npi response code was 200': res => res.status === 200,
        }
      );

      if (!checkFindOutput) {
        console.error(existingNpiResponse.body);
        exec.test.abort('failed find org by npi');
      }

      const existingOrgs =  existingNpiResponse.json();

      if ( existingOrgs.total == 0) {
        npi = npiToCheck;
        break;
      }
    }
    if (! npi) {
      exec.test.abort('failed to generate unused npi');
    }

    const org = createSmokeTestOrganization(npi, orgId, goldenMacaroon);

    const checkOutput = check(
      org,
      {
        'create org response code was 200': res => res.status === 200,
        'create org response has id': res => res.json().id,
      }
    );

    if (!checkOutput) {
      console.error(org.body);
      exec.test.abort('failed to create organizations on setup')
    }

  }

  return { orgIds: orgIds, tokens: tokens, goldenMacaroon: goldenMacaroon };
}

export async function bulkExportWorkflow(data) {
  const idx = (exec.vu.idInInstance % 3);

  const orgId = data.orgIds[idx];
  if (!orgId) {
    exec.test.abort('error indexing VU ID against orgIds array');
  }

  const token = data.tokens[idx];
  const uploadPractitionersResponse = createPractitionersFile(token, practitionerBundle);
  const checkUploadPractitioners = check(
    uploadPractitionersResponse,
    {
      'upload practitioners returns 200': res => res.status == 200,
      'uploaded four practitioners': res => res.json().entry.length == practitionerCount,
    }
  )

  if (!checkUploadPractitioners) {
    console.error(uploadPractitionersResponse.body);
  }
  const practitioners = uploadPractitionersResponse.json();

  const uploadPatientsResponse = createPatientsFile(token, patientBundle);
  const checkUploadPatients = check(
    uploadPatientsResponse,
    {
      'upload patients returns 200': res => res.status == 200,
      'uploaded 100 patients': res => res.json().entry.length == 100,
    }
  )
  if (!checkUploadPatients) {
    console.error(uploadPatientsResponse.body);
  }

  if (!checkUploadPractitioners || !checkUploadPatients) {
    exec.test.abort('Failed to upload practitioners and/or patients');
  }

  const patients = uploadPatientsResponse.json();
  createGroups(token, orgId, patients, practitioners);
  exportGroups(token, orgId, practitioners);
}

export function teardown(data) {
  for (const orgId of data.orgIds) {
    if (orgId) {
      deleteOrganization(orgId, data.goldenMacaroon);
    }
  }
}

function exportGroups(token, orgId, practitioners) {
  const practitionerMap = practitonerNpiPractitionerIdMap(practitioners);
  const badChecks = [];
  const groupIds = [];
  const groupNpiMap = {};
  for (const npi of Object.keys(practitionerMap)){
    const findGroupResponse = findGroupByPractitionerNpi(token, npi);
    const checkFindGroup = check(
      findGroupResponse,
      {
        'find group returns 200': res => res.status == 200,
        'found at least one group': res => res.json().total > 0,
      }
    );
    if (checkFindGroup) {
      const groupId = findGroupResponse.json().entry[0].resource.id;
      groupIds.push(groupId)
      groupNpiMap[groupId] = npi;
    } else {
      console.error(`Failed to find group for ${npi}`);
      badChecks.push(checkFindGroup);
    }
  }

  const jobUrls = [];
  for (const groupId of groupIds) {
    const getGroupExportResponse = exportGroup(token, groupId);
    const checkGetGroupExportResponse = check(
      getGroupExportResponse,
      {
        'response code was 202': res => res.status === 202,
        'has content-location header': res => res.headers['Content-Location'],
      },
    );
    if (checkGetGroupExportResponse) {
      const jobUrl = getGroupExportResponse.headers['Content-Location'];
      jobUrls.push(jobUrl);
    } else {
      console.log(`Unable to export group for ${groupNpiMap[groupId]}`);
      badChecks.push(checkGetGroupExportResponse);
    }
  }

  for (const jobUrl of jobUrls) {
    monitorJob(token, jobUrl, badChecks);
  }

  if (badChecks.length > 0) {
    exec.test.abort('Failed to export all groups.');
  }
}

function monitorJob(token, jobUrl, badChecks){
  let jobResponse = authorizedGet(token, jobUrl);
  while(jobResponse.status === 202){
    sleep(EXPORT_POLL_INTERVAL_SEC);
    jobResponse = authorizedGet(token, jobUrl);
  }

  try {
    const checkJobResponse = check(
      jobResponse,
      {
        'response code was 200': res => res.status === 200,
        'no job output errors': res => (res.json().error || 'errorlength').length <= JOB_OUPUT_ERROR_LENGTH,
      }
    );

    if (!checkJobResponse) {
      if (jobResponse.status == 401) {
        console.error('JOB TIMED OUT FOR TEST - MAYBE NOT FAIL');
      } else if (jobResponse.json().error) {
        console.error(`Too many errors in job output ${jobResponse.json().error.length}`);
      } else {
        console.error('Unable to check job output');
      }
      badChecks.push(checkJobResponse);
    }
  } catch (error) {
    console.error(`Error in ${jobResponse.body}`)
    console.error(error);
    badChecks.push(error);
  }
}

function createGroups(token, orgId, patients, practitioners) {
  const practitionerPatientMap = practitionerNpiPatientMbiMap();
  const patientMbiIdMap = patientMbiUUIDMap(patients);
  const practitionerMap = practitonerNpiPractitionerIdMap(practitioners);
  const badChecks = [];
  for (const npi of Object.keys(practitionerPatientMap)){
    const practitionerId = practitionerMap[npi];
    if (! practitionerId) {
      exec.test.abort(`Test misconfigured: missing practitioner ${npi}`);
    }
    const patientIds = []
    for(const mbi of practitionerPatientMap[npi]){
      const patientId = patientMbiIdMap[mbi];
      if (patientId){
        patientIds.push(patientId);
      }
    }
    if (patientIds.length == 0){
      exec.test.abort(`Test misconfigured: missing patients for ${npi}`);
    }
    const createGroupResponse = createGroupWithPatients(token, orgId, practitionerId, npi, patientIds);
    const checkCreateGroup = check(
      createGroupResponse,
      {
        'create group returns 201': res => res.status == 201,
      }
    )

    if (! checkCreateGroup){
      console.error(`Failed to create group for ${npi}`);
      badChecks.push(checkCreateGroup);
    }
  }
  if (badChecks.length > 0) {
    exec.test.abort('Failed to create all groups.');
  }

}

function practitonerNpiPractitionerIdMap(practitioners) {
  const map = {};
  for (const entry of practitioners.entry) {
    const id = entry.resource.id;
    let npi;
    for (const identifier of entry.resource.identifier){
      if (identifier.system == "http://hl7.org/fhir/sid/us-npi"){
        npi = identifier.value;
        break;
      }
    }
    if (id && npi){
      map[npi] = id;
    }
  }
  return map;
}

function practitionerNpiPatientMbiMap() {
  const practitionerPatientMap = {};
  const rows = associationsFile.split('\n');
  for (const row of rows) {
    const rowItems = row.split(',');
    if (rowItems && rowItems.length == 2) {
      practitionerPatientMap[rowItems[1]] ||= [];
      practitionerPatientMap[rowItems[1]].push(rowItems[0]);
    }
  }

  return practitionerPatientMap;
}

function patientMbiUUIDMap(patients){
  const patientMbiIdMap = {};
  for(const entry of patients.entry){
    const id = entry.resource.id;
    let mbi;
    for (const identifier of entry.resource.identifier){
      if (identifier.system == "http://hl7.org/fhir/sid/us-mbi"){
        mbi = identifier.value;
        break;
      }
    }
    if (id && mbi){
      patientMbiIdMap[mbi] = id;
    }
  }
  return patientMbiIdMap;
}
