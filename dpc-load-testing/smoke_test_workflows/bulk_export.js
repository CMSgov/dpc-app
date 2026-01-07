/*global console*/
/* eslint no-console: "off" */

import { check, sleep } from 'k6';
import exec from 'k6/execution'
import { generateDPCToken } from '../generate-dpc-token.js';
import {
  authorizedGet,
  createGroupWithPatients,
  createPatientsRawData,
  createPractitionersRawData,
  exportGroup,
  findGroupByPractitionerNpi,
  urlRoot,
} from '../dpc-api-client.js';


// We allow errors in local because we don't need to test our own connection to BFD
const JOB_OUPUT_ERROR_LENGTH = __ENV.ENVIRONMENT == 'local' ? 1 : 0;
// Our WAF rate limits us to 300 requests every 5 minutes, so don't poll too often
const EXPORT_POLL_INTERVAL_SEC = __ENV.ENVIRONMENT == 'local' ? 1 : 20;
const practitionerBundle =  __ENV.ENVIRONMENT == 'prod' ? open('./resources/prod_provider_bundle.json') : open('./resources/provider_bundle.json');
const patientBundle =  __ENV.ENVIRONMENT == 'prod' ? open('./resources/prod_patient_bundle-dpr.json') : open('./resources/patient_bundle-dpr.json');
const associationsFile =  __ENV.ENVIRONMENT == 'prod' ? open('./resources/prod_test_associations.csv') : open('./resources/test_associations-dpr.csv');
const practitionerCount =  __ENV.ENVIRONMENT == 'prod' ? 2 : 4;

// Sets up two test organizations
export async function checkBulkExportWorkflow(data) {
  const orgId = data.orgId;

  const token = generateDPCToken(orgId, data.goldenMacaroon);

  // Uploading Practitioners
  const uploadPractitionersResponse = createPractitionersRawData(token, practitionerBundle);
  const checkUploadPractitioners = check(
    uploadPractitionersResponse,
    {
      'upload practitioners returns 200': res => res.status == 200,
      'uploaded four practitioners': res => (res.json().entry || '').length == practitionerCount,
    }
  )
  if (!checkUploadPractitioners) {
    console.error(`Failed to upload practitioners ${uploadPractitionersResponse.body}`);
  }
  const practitioners = uploadPractitionersResponse.json();

  // Uploading Patients
  const uploadPatientsResponse = createPatientsRawData(token, patientBundle);
  const checkUploadPatients = check(
    uploadPatientsResponse,
    {
      'upload patients returns 200': res => res.status == 200,
      'uploaded 100 patients': res => (res.json().entry || '').length == 100,
    }
  )
  if (!checkUploadPatients) {
    console.error(`Failed to upload patients ${uploadPatientsResponse.body}`);
  }
  const patients = uploadPatientsResponse.json();

  // Abort if either upload failed
  if (!checkUploadPractitioners || !checkUploadPatients) {
    return;
  }


  // Holds all the errors so one error doesn't block others' attempts
  createGroups(token, orgId, patients, practitioners);

  exportGroups(token, orgId, practitioners);
}

function exportGroups(token, orgId, practitioners) {
  // Map of practitioner NPI to DPC-ID
  const practitionerMap = practitonerNpiPractitionerIdMap(practitioners);

  // Get the DPC-Group-IDs from practitioner's NPIs
  const groupNpiMap = {};
  for (const npi of Object.keys(practitionerMap)){
    const findGroupResponse = findGroupByPractitionerNpi(token, npi);
    const checkFindGroup = check(
      findGroupResponse,
      {
        'find group returns 200': res => res.status == 200,
        'found at least one group for practitioner': res => res.json().total > 0,
      }
    );
    if (checkFindGroup) {
      const groupId = findGroupResponse.json().entry[0].resource.id;
      groupNpiMap[groupId] = npi;
    } else {
      console.error(`Failed to find group for ${npi} -- status ${findGroupResponse.status}`);
    }
  }

  // Call the export api to get the jobs started
  const jobUrls = [];
  for (const groupId of Object.keys(groupNpiMap)) {
    const getGroupExportResponse = exportGroup(token, groupId);
    const checkGetGroupExportResponse = check(
      getGroupExportResponse,
      {
        'export response code was 202': res => res.status === 202,
        'export has content-location header': res => res.headers['Content-Location'],
      },
    );
    if (checkGetGroupExportResponse) {
      const jobUrl = getGroupExportResponse.headers['Content-Location'];
      jobUrls.push(jobUrl);
    } else {
      console.error(`Unable to export group for ${groupNpiMap[groupId]} -- ${getGroupExportResponse.status}`);
    }
  }

  // Verify the export worked
  for (const jobUrl of jobUrls) {
    monitorJob(token, jobUrl);
  }
}

function monitorJob(token, jobUrl){
  // Loop until it isn't 202
  // NB: because we are not refreshing the token, it will eventually get a 401
  let jobResponse = authorizedGet(token, jobUrl);
  while(jobResponse.status === 202){
    sleep(EXPORT_POLL_INTERVAL_SEC);
    jobResponse = authorizedGet(token, jobUrl);
  }

  // We got a rare exception when testing, so try is to make sure we capture the problem
  try {
    const checkJobResponse = check(
      jobResponse,
      {
        'job response code was 200': res => res.status === 200,
        'no job output errors': res => res.json().error.length <= JOB_OUPUT_ERROR_LENGTH,
      }
    );

    if (!checkJobResponse) {
      if (jobResponse.status == 401) {
        console.error(`JOB TIMED OUT FOR TEST - MAYBE NOT FAIL: ${jobUrl}`);
      } else if (jobResponse.json().error) {
        console.error(`Too many errors in job output ${jobResponse.json().error.length}: ${jobUrl}`);
      } else {
        console.error(`Bad response code when checking job output ${jobResponse.status} ${jobUrl}`);
      }
      return; // Exit early if job check failed
    }

    // Test file downloads for all three endpoints
    if (jobResponse.json().output && jobResponse.json().output.length > 0) {
      testFileDownloads(token, jobResponse.json().output);
    }
  } catch (error) {
    console.error(`Error thrown parsing ${jobResponse.body}: ${jobUrl}`)
    console.error(error);
    exec.test.fail();
  }
}

function testFileDownloads(token, outputFiles) {
  const baseUrl = urlRoot;
  
  for (const file of outputFiles) {
    if (!file.url) {
      continue;
    }

    // Extract fileID from URL (e.g., "abc-123-0.patient" from "http://.../Data/abc-123-0.patient.ndjson")
    const urlMatch = file.url.match(/\/Data\/(.+)\.ndjson$/);
    if (!urlMatch) {
      console.error(`Could not extract fileID from URL: ${file.url}`);
      continue;
    }
    
    const fileID = urlMatch[1];
    
    // Test uncompressed endpoint (original URL)
    const uncompressedUrl = `${baseUrl}/Data/${fileID}.ndjson`;
    const uncompressedResponse = authorizedGet(token, uncompressedUrl);
    const checkUncompressed = check(
      uncompressedResponse,
      {
        [`uncompressed download returns 200 for ${fileID}`]: res => res.status === 200,
        [`uncompressed download has content for ${fileID}`]: res => res.body && res.body.length > 0,
      }
    );
    if (!checkUncompressed) {
      console.error(`Failed to download uncompressed file ${fileID}: ${uncompressedResponse.status}`);
    }

    // Test compressed-decompressed endpoint
    const compressedDecompressedUrl = `${baseUrl}/Data/${fileID}.ndjson.gz`;
    const compressedDecompressedResponse = authorizedGet(token, compressedDecompressedUrl);
    const checkCompressedDecompressed = check(
      compressedDecompressedResponse,
      {
        [`compressed-decompressed download returns 200 for ${fileID}`]: res => res.status === 200,
        [`compressed-decompressed download has content for ${fileID}`]: res => res.body && res.body.length > 0,
        [`compressed-decompressed has ndjson content type for ${fileID}`]: res => res.headers['Content-Type'] === 'application/ndjson',
      }
    );
    if (!checkCompressedDecompressed) {
      console.error(`Failed to download compressed-decompressed file ${fileID}: ${compressedDecompressedResponse.status}`);
    }

    // Test compressed-raw endpoint
    const compressedRawUrl = `${baseUrl}/Data/${fileID}.ndjson.gz/raw`;
    const compressedRawResponse = authorizedGet(token, compressedRawUrl);
    const checkCompressedRaw = check(
      compressedRawResponse,
      {
        [`compressed-raw download returns 200 for ${fileID}`]: res => res.status === 200,
        [`compressed-raw download has content for ${fileID}`]: res => res.body && res.body.length > 0,
        [`compressed-raw has gzip content type for ${fileID}`]: res => res.headers['Content-Type'] === 'application/gzip',
      }
    );
    if (!checkCompressedRaw) {
      console.error(`Failed to download compressed-raw file ${fileID}: ${compressedRawResponse.status}`);
    }
  }
}

function createGroups(token, orgId, patients, practitioners) {
  // Map of practitioners to their patients
  const practitionerPatientMap = practitionerNpiPatientMbiMap();
  // Map of patient MBI to DPC-ID
  const patientMbiIdMap = patientMbiPatientIdMap(patients);
  // Map of practitioner NPI to DPC-ID
  const practitionerMap = practitonerNpiPractitionerIdMap(practitioners);

  // Create a group for each practitioner
  for (const npi of Object.keys(practitionerPatientMap)){
    const practitionerId = practitionerMap[npi];
    if (!practitionerId) {
      console.error(`Test misconfigured: missing practitioner ${npi}`);
      exec.test.fail();
      continue;
    }
    const patientIds = []
    for(const mbi of practitionerPatientMap[npi]){
      const patientId = patientMbiIdMap[mbi];
      if (patientId){
        patientIds.push(patientId);
      }
    }
    if (patientIds.length == 0){
      console.error(`Test misconfigured: missing patients for ${npi}`);
      exec.test.fail();
      continue;
    }
    const createGroupResponse = createGroupWithPatients(token, orgId, practitionerId, npi, patientIds);
    const checkCreateGroup = check(
      createGroupResponse,
      {
        'create group returns 201': res => res.status == 201,
      }
    )

    if (!checkCreateGroup){
      console.error(`Failed to create group for ${npi}: ${createGroupResponse.body}`);
    }
  }
}

// Build map of practitioner NPI to DPC-ID
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

// Build map of practitioners to their patients
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

// Build map of patient MBI to DPC-ID
function patientMbiPatientIdMap(patients){
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
