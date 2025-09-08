/*global console*/
/* eslint no-console: "off" */

import { check, fail, sleep } from 'k6';
import crypto from 'k6/crypto';
import exec from 'k6/execution'
import { fetchGoldenMacaroon, generateDPCToken } from './generate-dpc-token.js';
import {
  addPatientsToGroup,
  authorizedGet,
  createGroup,
  createGroupWithPatients,
  createOrganization,
  createPatientsBatch,
  createPractitioners,
  deleteOrganization,
  deletePatient,
  deletePractitioner,
  exportGroup,
  findGroupByPractitionerNpi,
  findOrganizationByNpi,
  findPatientByMbi,
  findPractitionerByNpi,
  getGroup,
  getOrganization,
  patientEverything,
  removePatientFromGroup,
  updateOrganization,
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
  thresholds: {
    checks: ['rate===1'],
  },
  scenarios: {
    workflow: {
      executor: 'per-vu-iterations',
      vus: constants.maxVUs,
      iterations: 1,
      exec: "workflow"
    }
  }
};

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
  // hard-coded to ensure proper data retrieval
  const mbis = ['1SQ3F00AA00', '5S58A00AA00', '4S58A00AA00', '3S58A00AA00', '0S80C00AA00']
  const orgId = data.orgIds[exec.vu.idInInstance];
  if (!orgId) {
    fail('error indexing VU ID against orgIds array');
  }

  const token = generateDPCToken(orgId, data.goldenMacaroon);

  // POST practitioner
  const practitionerNpi = '2459425221' // hard-coded for lookback tests
  const practitionerResponse = createPractitioners(token, practitionerNpi);
  const checkPractitionerResponse = check(
    practitionerResponse,
    {
      'status OK and fhir header': fhirOK,
      'practitioner id an npi': res => res.json().entry[0].resource.identifier[0].system === 'http://hl7.org/fhir/sid/us-npi',
    }
  );

  var practitionerId;
  if(checkPractitionerResponse) {
    // There's only 1 identifier in our synthetic practitioner, so we don't have to search for npi
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

  // GET patient by MBI
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

  var patientId;
  if (checkPatientByMbiRequest) {
    patientId = patientByMbiRequest.json().entry[0].resource.id;
  } else {
    console.error('Unable to find patient by mbi');
  }

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

  const getGroupResponse = findGroupByPractitionerNpi(token, practitionerNpi);
  const checkGetGroupResponse = check(
    getGroupResponse,
    {
      'status OK and fhir header': fhirOK,
      'is searchset': res => res.json().type === 'searchset',
      'one in searchset': res => res.json().total === 1,
    }
  );

  if (!checkGetGroupResponse) {
    console.error('Could not get group by NPI');
  }

  // PUT Remove Patient
  const removePatientResponse = removePatientFromGroup(token, orgId, practitionerId, practitionerNpi, groupId, patientId);
  const checkRemovePatientResponse = check(
    removePatientResponse,
    {
      'status OK and fhir header': fhirOK,
      'one inactive member': res => res.json().member.filter(member => member.inactive === true).length === 1,
      'four active member': res => res.json().member.filter(member => member.inactive === false).length === mbis.length - 1,
    }
  );
  if (!checkRemovePatientResponse){
    console.error('Could not remove patient');
  }

  // PUT Add Bad Patient
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
  if (!checkAddBadPatient){
    console.error('Add bad patient check failure');
  }

  // GET group export
  const getGroupExportResponse = exportGroup(token, groupId);
  const checkGetGroupExportResponse = check(
    getGroupExportResponse,
    {
      'response code was 202': res => res.status === 202,
      'has content-location header': res => res.headers['Content-Location'],
    },
  );

  if (checkGetGroupExportResponse) {
    // GET job response
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

      // GET patient data
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
      if (!checkPatientDataResponse){
	console.error('Patient data response failure');
      }

      // GET eob data
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
      if (!checkEobDataResponse){
	console.error('ExplanationOfBenefits data response failure');
      }
      // Get partial eob data
      const requestedByteCount = 10240;
      const partialRequestHeaders = {'Range': `bytes=0-${requestedByteCount}`};

      const partialEobDataResponse = authorizedGet(token, eob.url, partialRequestHeaders);

      const checkPartialEobDataResponse = check(
	partialEobDataResponse,
	{
	  'expect content length header to be requested': res => res.headers['Content-Length'] == requestedByteCount,
	  'expect body length to be requested': res => res.body.length === requestedByteCount,
	}
      )
      if (!checkPartialEobDataResponse){
	console.error('Parital ExplanationOfBenefits data response failure');
      }

      // Get coverage data
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
      if (!checkCoverageDataResponse){
	console.error('Coverage data response failure');
      }
      
      // GET operation outcome data
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
      if (!checkOperationOutcomeDataResponse){
	console.error('OperationOutcome data response failure');
      }
    }  else {
      console.error('Failed job response check; skipping data checks');
    }
  } else {
    console.error('Failed export; skipping job check');
  }


  // GET group export with _since
  const sinceDate = new Date().toISOString();

  const getGroupExportResponseWithSince = exportGroup(token, groupId, `_since=${sinceDate}`);
  const checkGetGroupExportResponseWithSince = check(
    getGroupExportResponseWithSince,
    {
      'response code was 202': res => res.status === 202,
      'has content-location header': res => res.headers['Content-Location'],
    },
  );

  if (checkGetGroupExportResponseWithSince) {
    // GET job response
    let jobUrl = getGroupExportResponseWithSince.headers['Content-Location'];
    let jobResponse = authorizedGet(token, jobUrl);
    while(jobResponse.status === 202){
      sleep(1);
      jobResponse = authorizedGet(token, jobUrl);
    }

    const checkJobResponseWithSince = check(
      jobResponse,
      {
	'response code was 200': res => res.status === 200,
	'0 errors': res => res.json().error.length === 0,
	'0 outputs': res => res.json().output.length === 0,
      }
    );
    if (!checkJobResponseWithSince){
      console.error('Job with since failure');
    }
  } else {
    console.error('Failed export with since; skipping job check');
  }

  // GET patient everything
  const patientEverythingResponse = patientEverything(token, orgId, practitionerId, patientId);

  const checkPatientEverythingResponse = check(
    patientEverythingResponse,
    {
      'response code was 200': res => res.status === 200,
      'response type bundle': res => res.json().resourceType === 'Bundle',
      'one patient': res => res.json().entry.filter(e => e.resource.resourceType === 'Patient').length === 1,
      '4 coverages': res => res.json().entry.filter(e => e.resource.resourceType === 'Coverage').length === 4,
      '10 eobs': res => res.json().entry.filter(e => e.resource.resourceType === 'ExplanationOfBenefit').length === 10,
    }
  );
  if (!checkPatientEverythingResponse){
    console.error('Patient everything failure');
  }
  
  // GET Organization
  const getOrganizationResponse = getOrganization(token);
  const checkGetOrganizationResponse = check(
    getOrganizationResponse,
    {
      'status OK and fhir header': fhirOK,
    }
  );
  if (!checkGetOrganizationResponse){
    console.error('Get organization for update failure');
  }
  const organization = getOrganizationResponse.json().entry[0].resource;

  
  // PUT Organization Tests
  const newName = 'New Name';
  organization['name'] = newName;
  const newAddress = [
    {
      "city": "Chestnut Hill",
      "country": "US",
      "line": [
        "200 Boylston Street, 4th Floor",
        "Suite 66"
      ],
      "postalCode": "02467",
      "state": "MA",
      "type": "both",
      "use": "work"
    }
  ];
  organization['address'] = newAddress;

  // PUT Organization with bad content type
  const updateOrganizationResponseBadCT = updateOrganization(token, organization, 'application/fire+json');
  const expectedIssueText = '`Content-Type:` header must specify valid FHIR content type'

  const checkUpdateOrganizationResponseBadCT = check(
    updateOrganizationResponseBadCT,
    {
      'response code was 415': res => res.status === 415,
      'one issue': res => res.json().issue.length === 1,
      'issue text matches': res => res.json().issue[0].details.text === expectedIssueText,
    }
  );
  if (!checkUpdateOrganizationResponseBadCT){
    console.error('Bad content type check failure');
  }

  // PUT Organization with good content type
  const updateOrganizationResponse = updateOrganization(token, organization);
  const checkUpdateOrganizationResponse = check(
    updateOrganizationResponse,
    {
      'response code was 200': res => res.status === 200,
      'name updated': res => res.json().name === newName,
      'street updated': res => res.json().address[0].line[0] === "200 Boylston Street, 4th Floor",
      'city updated': res => res.json().address[0].city === "Chestnut Hill",
      'state updated': res => res.json().address[0].state === "MA",
      'zip updated':  res => res.json().address[0].postalCode === "02467",
    }
  );
  if (!checkUpdateOrganizationResponse){
    console.error('Organization update failure');
  }

  // GET Practitioner by NPI
  const practitionerByNpiResponse = findPractitionerByNpi(token, practitionerNpi);
  const checkPractitionerByNpiResponse = check(
    practitionerByNpiResponse,
    {
      'status OK and fhir header': fhirOK,
      'type is searchset': res => res.json().type === 'searchset',
      'one practitioner': res => res.json().total === 1,
    }
  );
  if (!checkPractitionerByNpiResponse){
    console.error('Get practitioner by NPI failure');
  }

  // DELETE Patient
  const deletePatientResponse = deletePatient(token, patientId);
  const checkDeletePatientResponse = check(
    deletePatientResponse,
    {
      'response code was 200': res => res.status === 200,      
    }
  );
  if (!checkDeletePatientResponse){
    console.error('Delete patient failure');
  }

  // GET Roster (make sure patient removed)
  const verifyPatientMissing = function(body) {
    if (body.member.length != mbis.length - 1){
      console.error(`Should have ${mbis.length - 1} but have ${body.member.length}`);
      return false;
    }
    var pass = true;
    body.member.forEach((member) => {
      if (!member.entity.reference) {
	console.error('Missing entity reference');
	pass = false;
      }
      if (member.entity.reference === `Patient/${patientId}`) {
	console.error('Patient still present in group');
	pass = false;
      }
    });
    return pass;
  }

  const getRosterAfterDeletionResponse = getGroup(token, groupId);
  const checkGetRosterAfterDeletionResponse = check(
    getRosterAfterDeletionResponse,
    {
      'status OK and fhir header': fhirOK,
      'patient missing': res => verifyPatientMissing(res.json()),
      
    }
  );
  if (!checkGetRosterAfterDeletionResponse){
    console.error('Roster after patient delete failure');
  }
  
  // DELETE Practitioner
  const deletePractitionerResponse = deletePractitioner(token, practitionerId);
  const checkDeletePractitionerResponse = check(
    deletePractitionerResponse,
    {
      'response code was 200': res => res.status === 200,      
    }
  );
  if (!checkDeletePractitionerResponse){
    console.error('Delete practitioner failure');
  }

  const getGroupAfterPractitionerDeleteResponse = findGroupByPractitionerNpi(token, practitionerNpi);
  const checkGetGroupAfterPractitionerDeleteResponseetGroupResponse = check(
    getGroupAfterPractitionerDeleteResponse,
    {
      'status OK and fhir header': fhirOK,
      'is searchset': res => res.json().type === 'searchset',
      'one in searchset': res => res.json().total === 0,
    }
  );
  if (!checkGetGroupAfterPractitionerDeleteResponseetGroupResponse){
    console.error('Check for roster after practitioner delete failure');
  }

  // GET Organization gzipped
  const getOrganizationGzippedResponse = getOrganization(token, true);
  const checkGetOrganizationGzippedResponse = check(
    getOrganizationGzippedResponse,
    {
      'status OK and fhir header': fhirOK,
      'content encoding has gzip': res => res.headers['Content-Encoding'] === 'gzip',
    }
  );if (!getOrganizationGzippedResponse){
    console.error('Gzipped organization failure');
  }
}

export function teardown(data) {
  for (const orgId of data.orgIds) {
    if (orgId) {
      deleteOrganization(orgId, data.goldenMacaroon);
    }
  }
}
