/*global console*/
/* eslint no-console: "off" */

import { check, sleep } from 'k6';
import { generateDPCToken } from '../generate-dpc-token.js';
import {
  authorizedGet,
  createGroupWithPatients,
  createPatientsBatch,
  patientEverything,
  createPractitioners,
} from '../dpc-api-client.js';

// Our WAF rate limits us to 300 requests every 5 minutes, so don't poll too often
const EXPORT_POLL_INTERVAL_SEC = __ENV.ENVIRONMENT == 'local' ? 1 : 20;
const practitionerNpi = __ENV.ENVIRONMENT == 'prod' ? "1234329724" : "3247281157";

export async function checkPatientEverythingExportWorkflow(data) {
  const orgId = data.orgId;
  const token = generateDPCToken(orgId, data.goldenMacaroon);

  // Uploading practitioner
  const createPractitionerResponse = createPractitioners(token, practitionerNpi);
  const checkUploadPractitioners = check(
    createPractitionerResponse,
    {
      'upload single practitioner returns 200': res => res.status == 200,
      'uploaded one practitioner': res => (res.json().entry || '').length == 1,
    }
  )
  if (!checkUploadPractitioners) {
    console.error(`Failed to upload single practitioner ${createPractitionerResponse.body}`);
  }

  // Uploading Patient
  const patientMbi = "1SQ3F00AA00"; // valid BFD patient 
  const uploadPatientResponse = createPatientsBatch(token, [patientMbi]);
  const checkUploadPatient = check(
    uploadPatientResponse,
    {
      'upload single patient returns 200': res => res.status == 200,
      'uploaded one patient': res => (res.json().entry || '').length == 1,
    }
  )
  if (!checkUploadPatient) {
    console.error(`Failed to upload patients ${uploadPatientResponse.body}`);
  }

  // Abort if either upload failed
  if (!checkUploadPractitioners || !checkUploadPatient) {
    return;
  }

  // Create Group for practitioner and patient
  const practitionerId = createPractitionerResponse.json().entry[0].resource.id;
  const patientId = uploadPatientResponse.json().entry[0].resource.id;

  const createGroupResponse = createGroupWithPatients(token, orgId, practitionerId, practitionerNpi, [patientId]);
  const checkCreateGroup = check(
      createGroupResponse,
      {
      'create group returns 201': res => res.status == 201,
      }
  )

  if (!checkCreateGroup){
      console.error(`Failed to create group for ${practitionerNpi}: ${createGroupResponse.body}`);
  }
  
  // Patient everything export
  const patientEverythingResponse = patientEverything(token, orgId, practitionerId, patientId);
  const checkPatientEverything = check(
    patientEverythingResponse,
    {
      'get patient everything returns 200': res => res.status == 200,
    }
  )

  if (!checkPatientEverything){
    console.error(`Failed to call patient everything for ${patientId}: ${patientEverythingResponse.body}`);
  }

  // Patient everything export async
  const patientEverythingAsyncResponse = patientEverything(token, orgId, practitionerId, patientId, "respond-async");
  const checkPatientEverythingAsync = check(
    patientEverythingAsyncResponse,
    {
      'get patient everything async returns 202': res => res.status == 202,
      'response location header present': (res) => !!(res?.headers['Content-Location']),
    }
  )

  if (!checkPatientEverythingAsync) {
    console.error(`Failed to call patient everything async for ${patientId}: ${patientEverythingAsyncResponse.body}`);
  }

  // Verify patient everything job succeeds
  const jobUrl = patientEverythingAsyncResponse.headers['Content-Location'];
  const pollJobStatusResponse = pollJobStatus(token, jobUrl);
  const checkPollJobStatus = check(
    pollJobStatusResponse,
    {
      'get patient everything async job status returned 200': res => res.status == 200,
    }
  )

  if (!checkPollJobStatus) {
    console.error(`Polling failed with status: ${checkPollJobStatus.status}`);
  }

}

function pollJobStatus(token, jobUrl) {
  let retryCount = 0;
  const maxRetries = 10;
  
  while (retryCount < maxRetries) {
    sleep(EXPORT_POLL_INTERVAL_SEC); 
    
    const res = authorizedGet(token, jobUrl);
    
    if (res.status === 200) {
      console.log('Job complete!');
      return res;
    } else if (res.status === 202) {
      console.log('Job still processing...');
      retryCount++;
    } else {
      console.error('Error while polling job status');
      break;
    }
  }
}