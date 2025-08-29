/*global console*/
/* eslint no-console: "off" */

import { check, fail, sleep } from 'k6';
import crypto from 'k6/crypto';
import exec from 'k6/execution'
import { fetchGoldenMacaroon, generateDPCToken } from './generate-dpc-token.js';
import {
  createGroup,
  createGroupWithPatients,
  createOrganization,
  createPractitioners,
  deleteOrganization,
  exportGroup,
  findOrganizationByNpi,
  findGroupByPractitionerNpi,
  createPatientsBatch,
  removePatientFromGroup,
  findPatientByMbi,
  addPatientsToGroup,
  authorizedGet,
} from './dpc-api-client.js';
import NPIGeneratorCache from './utils/npi-generator.js';
import MBIGeneratorCache from './utils/mbi-generator.js';
import { constants } from './constants.js';

const npiGeneratorCache = new NPIGeneratorCache();
const mbiGeneratorCache = new MBIGeneratorCache();
const fhirType = 'application/fhir+json';
const fhirOK = function(res) {
  return res.status === 200 && res.headers['Content-Type'] === fhirType;
};

export const options = {
  scenarios: {
    workflow: {
      executor: 'per-vu-iterations',
      vus: constants.maxVUs,
      iterations: 1,
      exec: "workflow"
    }
  }
};

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
	'status OK and fhir header': fhirOK,
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
	'accept header fhir type': res => res.headers['Content-Type'] === fhirType,
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
  const mbis = ['1SQ3F00AA00', '5S58A00AA00', '4S58A00AA00', '3S58A00AA00', '0S80C00AA00']
  const orgId = data.orgIds[exec.vu.idInInstance];
  if (!orgId) {
    fail('error indexing VU ID against orgIds array');
  }


  const token = generateDPCToken(orgId, data.goldenMacaroon);

  // POST practitioner
  const practitionerResponse = createPractitioners(token, '2459425221');
  const checkPractitionerResponse = check(
    practitionerResponse,
    {
      'status OK and fhir header': fhirOK,
      'practitioner id an npi': res => res.json().entry[0].resource.identifier[0].system === 'http://hl7.org/fhir/sid/us-npi',
    }
  );

  var practitionerNpi, practitionerId;
  if(checkPractitionerResponse) {
    // There's only 1 identifier in our synthetic practitioner, so we don't have to search for npi
    practitionerNpi = practitionerResponse.json().entry[0].resource.identifier[0].value;
    practitionerId = practitionerResponse.json().entry[0].resource.id;
  } else {
    console.error('failed to create practitioners');
  }
  // POST patients
  const patientsResponse = createPatientsBatch(token, mbis);
  const checkPatientsResponse = check(
    patientsResponse,
    {
      'status OK and fhir header': fhirOK,
      'created patients': res => res.json().entry.length === mbis.length,
    }
  );

  const patients = [];
  if(checkPatientsResponse) {
    patientsResponse.json().entry.forEach((entry) => patients.push(entry.resource.id));
  } else {
    console.error('failed to create patients');
  }

  const patientByMbiRequest = findPatientByMbi(token, mbis[0]);
  const checkPatientByMbiRequest = check(
    patientByMbiRequest,
    {
      'status OK and fhir header': fhirOK,
      'is searchset': res => res.json().type === 'searchset',
      'one in searchset': res => res.json().total === 1,
      'system is mbi': res => res.json().entry[0].resource.identifier[0].system === 'http://hl7.org/fhir/sid/us-mbi',
    }
  )

  // POST group for practitioner

  const createGroupResponse = createGroupWithPatients(token, orgId, practitionerId, practitionerNpi, patients);

  const memberContentVerified = function(res) {
    var pass = true;
    res.json().member.forEach((patient) => {
      if (!patients.includes(patient.entity.reference.slice(8))){
	pass = false;
      }
      if (!patient.period.start || patient.period.start === patient.period.end) {
	pass = false;
      }
    });
    return pass;
  }

  const checkCreateGroupResponse = check(
    createGroupResponse,
    {
      'response code was 201': res => res.status === 201,
      'accept header fhir type': res => res.headers['Content-Type'] === fhirType,
      'correct number of patients': res => res.json().member.length === mbis.length,
      'member content verified': memberContentVerified,
    }
  );

  var groupId;
  if (checkCreateGroupResponse) {
    groupId = createGroupResponse.json().id;
  } else {
    console.error('Could not create group');
  }

  var patientId;
  if (checkPatientByMbiRequest) {
    patientId = patientByMbiRequest.json().entry[0].resource.id;
  } else {
    console.error('Unable to find patient by mbi');
  }

  const getGroupResponse = findGroupByPractitionerNpi(token, practitionerNpi);
  const checkGetGroupResponse = check(
    getGroupResponse,
    {
      'status OK and fhir header': fhirOK,
      'is searchset': res => res.json().type === 'searchset',
      'one in searchset': res => res.json().total === 1,
    }
  );

  const removePatientResponse = removePatientFromGroup(token, orgId, practitionerId, practitionerNpi, groupId, patientId);
  const checkRemovePatientResponse = check(
    removePatientResponse,
    {
      'status OK and fhir header': fhirOK,
      'one inactive member': res => res.json().member.filter(member => member.inactive === true).length === 1,
      'four active member': res => res.json().member.filter(member => member.inactive === false).length === mbis.length - 1,
    }
  );

  const badPatient = { 'patientId': 'c22044f0-3b8e-488c-bcd4-fcbc630d9c19' };
  const badPatientErrorMessage = 'All patients in group must exist. Cannot find 1 patient(s).'
  const addBadPatient = addPatientsToGroup(token, orgId, groupId, [badPatient], practitionerId, practitionerNpi)[0];

  const checkAddBadPatient = check(
    addBadPatient,
    {
      'response code was 400': res => res.status === 400,
      'accept header fhir type': res => res.headers['Content-Type'] === fhirType,
      'one entry': res => res.json().issue.length === 1,
      'correct error message': res => res.json().issue[0].details.text === badPatientErrorMessage,
    }
  );

  // GET group export
  const findJobUrls = []
  const getGroupExportResponse = exportGroup(token, groupId);
  const checkGetGroupExportResponse = check(
    getGroupExportResponse,
    {
      'response code was 202': res => res.status === 202,
      'has content-location header': res => res.headers['Content-Length'],
    },
  );

  if (checkGetGroupExportResponse) {
    let jobUrl = getGroupExportResponse.headers['Content-Location'];
    let jobResponse = authorizedGet(token, jobUrl);
    while(jobResponse.status === 202){
      sleep(1);
      jobResponse = authorizedGet(token, jobUrl);
    }

    const checkExtension = function(obj) {
      return obj.extension.length === 2 &&
	obj.extension[0].url === 'https://dpc.cms.gov/checksum' &&
	obj.extension[0].valueString  &&
	obj.extension[1].url === 'https://dpc.cms.gov/file_length' &&
	obj.extension[1].valueDecimal;
    };

    const checkJobResponse = check(
      jobResponse,
      {
	'response code was 200': res => res.status === 200,
	'one error': res => res.json().error.length === 1,
	'three outputs': res => res.json().output.length === 3,
	'three patients': res => res.json().output.filter((elem) => elem.type == "Patient")[0].count === 3,
	'patient extention': res => checkExtension(res.json().output.filter((elem) => elem.type == "Patient")[0]),
	'more than 100 eobs': res => res.json().output.filter((elem) => elem.type == "ExplanationOfBenefit")[0].count > 100,
	'eob extention': res => checkExtension(res.json().output.filter((elem) => elem.type == "ExplanationOfBenefit")[0]),
	'twelve coverages': res => res.json().output.filter((elem) => elem.type == "Coverage")[0].count === 12,
	'coverage extention': res => checkExtension(res.json().output.filter((elem) => elem.type == "Coverage")[0]),
	'one operation outcome': res => res.json().error.filter((elem) => elem.type == "OperationOutcome")[0].count === 1,
	'operation outcome extention': res => checkExtension(res.json().error.filter((elem) => elem.type == "OperationOutcome")[0]),
	'has expires header': res => Date.parse(res.headers.Expires),
	'does not expire early': res => (Date.parse(res.headers.Expires) - Date.now())/3600000 > 23,
	'does not expire late': res => (Date.parse(res.headers.Expires) - Date.now())/3600000 < 24,
      }
    );

    if (checkJobResponse) {
      const patient = jobResponse.json().output.filter((elem) => elem.type == "Patient")[0];
      const patientChecksum = patient.extension[0].valueString;
      const patientDataResponse = authorizedGet(token, patient.url);

      const verifyPatientData = function(body) {
	const patientBlocks = body.trim().split('\n');
	if (patientBlocks.length != 3) return false;
	var pass = true;
	patientBlocks.forEach((block) => {
	  const blockData = JSON.parse(block);
	  if (blockData.resourceType != 'Patient') pass = false;
	  const mbiStanza = blockData.identifier.find(i => i.system === "http://hl7.org/fhir/sid/us-mbi");
	  if (mbiStanza === undefined) pass = false;
	});
	const checksum  = crypto.sha256(body, 'hex');
	if (`sha256:${checksum}` != patientChecksum) pass = false;
	return pass;
      }
      const checkPatientDataResponse = check(
	patientDataResponse,
	{
	  'response code was 200': res => res.status === 200,
	  'has content type ndjson': res => res.headers['Content-Type'] === 'application/ndjson',
	  'has correct patient data': res => verifyPatientData(res.body),
	}
      );

      const eob = jobResponse.json().output.filter((elem) => elem.type == "ExplanationOfBenefit")[0];

      const eobDataResponse = authorizedGet(token, eob.url);
      const verifyEobData = function(body) {
	const eobBlocks = body.trim().split('\n');
	if (eobBlocks.length < 100) return false;
	var pass = true;
	eobBlocks.forEach((block) => {
	  const blockData = JSON.parse(block);
	  if (blockData.resourceType != 'ExplanationOfBenefit') pass = false;
	});
	return pass;
      }
      const checkEobDataResponse = check(
	eobDataResponse,
	{
	  'response code was 200': res => res.status === 200,
	  'has content type ndjson': res => res.headers['Content-Type'] === 'application/ndjson',
	  'has correct eob data': res => verifyEobData(res.body),
	}
      );
      
      
      const coverage = jobResponse.json().output.filter((elem) => elem.type == "Coverage")[0];
      const coverageChecksum = coverage.extension[0].valueString;
      const coverageDataResponse = authorizedGet(token, coverage.url);

      const verifyCoverageData = function(body) {
	const coverageBlocks = body.trim().split('\n');
	if (coverageBlocks.length != 12) return false;
	var pass = true;
	coverageBlocks.forEach((block) => {
	  const blockData = JSON.parse(block);
	  if (blockData.resourceType != 'Coverage') pass = false;
	});
	const checksum  = crypto.sha256(body, 'hex');
	if (`sha256:${checksum}` != coverageChecksum) pass = false;
	return pass;
      }
      const checkCoverageDataResponse = check(
	coverageDataResponse,
	{
	  'response code was 200': res => res.status === 200,
	  'has content type ndjson': res => res.headers['Content-Type'] === 'application/ndjson',
	  'has correct coverage data': res => verifyCoverageData(res.body),
	}
      );
      
      const operationOutcome = jobResponse.json().error[0];
      const operationOutcomeChecksum = operationOutcome.extension[0].valueString;
      const operationOutcomeDataResponse = authorizedGet(token, operationOutcome.url);

      const verifyOperationOutcomeData = function(body) {
	const operationOutcomeBlocks = body.trim().split('\n');
	if (operationOutcomeBlocks.length != 1) return false;
	var pass = true;
	operationOutcomeBlocks.forEach((block) => {
	  const blockData = JSON.parse(block);
	  if (blockData.resourceType != 'OperationOutcome') pass = false;
	  const issue = blockData.issue[0];
	  if (issue.details.text != 'Unable to retrieve patient data due to internal error') {
	    pass = false;
	  }
	  if (!issue.location.includes('0S80C00AA00')) pass = false;
	});
	const checksum  = crypto.sha256(body, 'hex');
	if (`sha256:${checksum}` != operationOutcomeChecksum) pass = false;
	return pass;
      }
      const checkOperationOutcomeDataResponse = check(
	operationOutcomeDataResponse,
	{
	  'response code was 200': res => res.status === 200,
	  'has content type ndjson': res => res.headers['Content-Type'] === 'application/ndjson',
	  'has correct operationOutcome data': res => verifyOperationOutcomeData(res.body),
	}
      );
    }  else {
      console.log('Failed job response check; skipping data checks');
    }
  } else {
    console.error('Failed export; skipping job check');
  }
}

export function teardown(data) {
  for (const orgId of data.orgIds) {
    if (orgId) {
      deleteOrganization(orgId, data.goldenMacaroon);
    }
  }
}
