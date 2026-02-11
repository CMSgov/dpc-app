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
  patientEverything,
  createPractitioners,
} from '../dpc-api-client.js';

// Update with Prod Examples 
const singlePatient =  __ENV.ENVIRONMENT == 'prod' ? open('./resources/single_patient.json') : open('./resources/single_patient.json');

// Sets up two test organizations
export async function checkPatientEverythingExportWorkflow(data) {
  const orgId = data.orgId;
  const token = generateDPCToken(orgId, data.goldenMacaroon);
  const practitionerNpi = "1234329724";

  // Uploading single practitioner
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

  // Uploading Patients
  const patientMbi = "1SQ3F00AA00";
  const uploadPatientResponse = createPatientsRawData(token, singlePatient);
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

}

