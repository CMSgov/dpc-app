/*global console*/
/* eslint no-console: "off" */

import { check } from 'k6';
import { generateDPCToken } from '../generate-dpc-token.js';
import {
  createPatientsBatch,
  patientEverything,
  createPractitioners,
} from '../dpc-api-client.js';
import {
  monitorJob,
} from './smoke_test_utils.js';

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
  monitorJob(token, jobUrl);

}
