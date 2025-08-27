#!/usr/bin/env python
"""
Runs end-to-end test of API
"""

from datetime import datetime, UTC
import hashlib
import json
import pathlib
import sys
import time
from urllib import request
from urllib.error import URLError

WORKING_DIR = pathlib.Path(__file__).parent.resolve()

API_BASE = 'http://localhost:3002/api/v1/'

FHIR_TYPE = 'application/fhir+json'

FHIR_HEADERS = {'Accept': FHIR_TYPE, 'Content-Type': FHIR_TYPE}

class ExpectationException(Exception):
    def __init__(self, expected, actual, msg=None):
        self.expected = expected
        self.actual = actual
        self.msg = msg
        if msg:
            prefix = f'{msg}: '
        else:
            prefix = ''
        super().__init__(f'{prefix}Expected {expected} | Actual {actual}')

def dig(_dict, *keys):
    try:
        for key in keys:
            _dict = _dict[key]
        return _dict
    except (KeyError, IndexError):
        return None

def attestation(org_id, provider_id):
    return json.dumps({ "resourceType":"Provenance",
                        "meta":{ "profile":[ "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-attestation" ] },
                        "recorded": datetime.now(UTC).isoformat(),
                        "reason":[ { "system":"http://hl7.org/fhir/v3/ActReason", "code":"TREAT" } ],
                        "agent":[ { "role":[ { "coding":[ { "system":"http://hl7.org/fhir/v3/RoleClass", "code":"AGNT" } ] } ],
                                    "whoReference":{ "reference":f"Organization/{org_id}" },
                                    "onBehalfOfReference":{ "reference":f"Practitioner/{provider_id}" } } ] })

def fhir_headers_with_attestation(org_id, provider_id):
    headers = FHIR_HEADERS.copy()
    headers['X-Provenance'] = attestation(org_id, provider_id)

    return headers

def async_fhir_headers():
    headers = FHIR_HEADERS.copy()
    headers['Prefer'] = 'respond-async'

    return headers

def get(url, **kwargs):
    """
    keyword args:
    headers
    response_test
    error_test
    method
    """
    method = kwargs.get('method', 'GET')
    req = request.Request(url, method=method)
    for key, value in kwargs.get('headers', {}).items():
        req.add_header(key, value)
    try:
        with request.urlopen(req) as resp:
            body = resp.read().decode('utf-8')
            return kwargs['response_test'](resp, body)
    except KeyError as ke:
        if 'error_test' in kwargs:
            raise ExpectationException('Bad HTTP status', resp.status) from ke
        raise ke
    except URLError as e:
        if 'error_test' in kwargs:
            return kwargs['error_test'](e)
        raise ExpectationException('No error', e) from e
    return None

def delete(url):
    return get(url, method='DELETE', response_test=lambda x,y: None)

def post(url, request_body, **kwargs):
    """
    keyword args:
    headers
    response_test
    error_test
    method
    """
    jsondata = json.dumps(request_body)
    jsondataasbytes = jsondata.encode('utf-8')
    method = kwargs.get('method', 'POST')
    req = request.Request(url, method=method)
    for key, value in kwargs.get('headers', {}).items():
        req.add_header(key, value)
    try:
        with request.urlopen(req, jsondataasbytes) as resp:
            body = resp.read().decode('utf-8')
            return kwargs['response_test'](resp, body)
    except KeyError as ke:
        if 'error_test' in kwargs:
            raise ExpectationException('Bad HTTP status', resp.status) from ke
        raise ke
    except URLError as e:
        if 'error_test' in kwargs:
            return kwargs['error_test'](e)
        raise ExpectationException('No error', e) from e
    return None

def put(url, request_body, **kwargs):
    return post(url, request_body, method='PUT', **kwargs)

def bundle(name):
    with open(f'{WORKING_DIR}/resources/e-2-e-bundles/{name}_bundle.json') as f:
        return json.load(f)

def match_fhir_ok(resp):
    match_eq(resp.status, 200)
    match_eq(resp.headers['content-type'], FHIR_TYPE)

def match_ndjson_ok(resp):
    match_eq(resp.status, 200)
    match_eq(resp.headers['content-type'], 'application/ndjson')

def match_eq(actual, expect, msg=None):
    if actual != expect:
        raise ExpectationException(expect, actual, msg)

def match_ne(actual, expect):
    if actual == expect:
        raise ExpectationException(f'{actual} != {expect}', 'equality')

def match_truth(actual, obj):
    if not actual:
        raise ExpectationException(f'{obj} to be truthy', f'was not {actual}')

def match_sha(body, sha):
    m = hashlib.sha256()
    m.update(body.encode('utf-8'))
    match_eq(f'sha256:{m.hexdigest()}', sha)

def match_valid_extension(obj):
    """ From EndToEndRequestTest (patient example)
    pm.expect(Object.keys(patient.extension[0]).length).to.equal(2);
    pm.expect(patient.extension[0].url).to.equal("https://dpc.cms.gov/checksum");
    pm.expect(patient.extension[0].valueString).to.exist;
    pm.expect(Object.keys(patient.extension[1]).length).to.equal(2);
    pm.expect(patient.extension[1].url).to.equal("https://dpc.cms.gov/file_length");
    pm.expect(patient.extension[1].valueDecimal).to.exist;
    pm.environment.set("patient", patient);
    """
    match_eq(len(obj['extension']), 2)
    match_eq(len(dig(obj, 'extension', 0)), 2)
    match_eq(dig(obj, 'extension', 0, 'url'), 'https://dpc.cms.gov/checksum')
    match_truth(dig(obj, 'extension', 0, 'valueString'), 'extension valueString')
    match_eq(len(dig(obj, 'extension', 1)), 2)
    match_eq(dig(obj, 'extension', 1, 'url'), 'https://dpc.cms.gov/file_length')
    match_truth(dig(obj, 'extension', 1, 'valueDecimal'), 'extension valueDecimal')

def check_for_roster():
    url = API_BASE + 'Group?characteristic-value=attributed-to$2459425221'
    def response_test(resp, body):
        match_fhir_ok(resp)
        roster = json.loads(body)
        return dig(roster, 'entry', 0, 'resource', 'id')

    return get(url, headers=FHIR_HEADERS, response_test=response_test)

# TESTS
def create_organization():
    """ From EndToEndRequestTest
    pm.test("Status is 201", function () {
        pm.response.to.have.status(201);
    });

    // Response should have FHIR Content-Type
    pm.test("Content-type is application/fhir+json", function() {
        pm.response.to.have.header("Content-Type", "application/fhir+json");
    });

    pm.globals.set("organization_id", pm.response.json().id);
    """
    url = API_BASE + 'Organization/$submit'
    org_bundle = bundle('organization')
    def response_test(resp, body):
        match_fhir_ok(resp)
        org = json.loads(body)
        return org['id']
    return post(url, org_bundle, headers=FHIR_HEADERS, response_test=response_test)

def register_providers():
    """ From EndToEndRequestTest
    pm.test("Status is 200", function () {
        pm.response.to.have.status(200);
    });

    // Response should have FHIR Content-Type
    pm.test("Content-type is application/fhir+json", function() {
       pm.response.to.have.header("Content-Type", "application/fhir+json");
    });

    // Parse the Bundle and get a list of practitioner ids
    pm.test("Practitioner bundle correct", function() {
        pm.response.to.be.ok;
        var responseOutput = pm.response.json();
        pm.expect(responseOutput.entry.length).to.equal(1);
        pm.expect(responseOutput.entry[0].resource.identifier[0].system).to.equal("http://hl7.org/fhir/sid/us-npi");
        pm.environment.set("provider_npi", responseOutput.entry[0].resource.identifier[0].value);
        pm.environment.set("provider_id", responseOutput.entry[0].resource.id)
    });
    """
    url = API_BASE + 'Practitioner/$submit'
    providers_bundle = bundle('providers')
    def response_test(resp, body):
        match_fhir_ok(resp)
        providers = json.loads(body)
        return [entry['resource']['id'] for entry in providers['entry']]
    return post(url, providers_bundle, headers=FHIR_HEADERS, response_test=response_test)

def register_patients():
    """ From EndToEndRequestTest
    // Status should be 200
    pm.test("Status is 200", function () {
        pm.response.to.have.status(200);
    });

    // Response should have FHIR Content-Type
    pm.test("Content-type is application/fhir+json", function() {
       pm.response.to.have.header("Content-Type", "application/fhir+json");
    });

    // Parse the Bundle and get a list of practitioner ids
    pm.test("Patient bundle correct", function() {
        pm.response.to.be.ok;
        var responseOutput = pm.response.json();
        pm.expect(responseOutput.entry.length).to.equal(5);

        var patientIDs = [];
        responseOutput.entry.forEach(function (entry) {patientIDs.push("Patient/" + entry.resource.id)});
        console.log(patientIDs);
        pm.globals.set("patient_ids", JSON.stringify(patientIDs));
    });
    """
    url = API_BASE + 'Patient/$submit'
    patients_bundle = bundle('patients')
    def response_test(resp, body):
        match_fhir_ok(resp)
        patients = json.loads(body)
        match_eq(len(dig(patients, 'entry') or []), 5)
        return [dig(patient, 'resource', 'id') for patient in dig(patients, 'entry')]
    return post(url, patients_bundle, headers=FHIR_HEADERS, response_test=response_test)

def submit_roster(org_id, provider_id, patient_ids):
    """ From EndToEndRequestTest
    // Status should be 201
    pm.test("Status is 201", function () {
        pm.response.to.have.status(201);
    });

    // Response should have FHIR Content-Type
    pm.test("Content-type is application/fhir+json", function() {
       pm.response.to.have.header("Content-Type", "application/fhir+json");
    });

    pm.test("Response Body is correct", () => {
        console.log("Body correct?");
        // Check to ensure that each member id matches what we previously had
        var response = pm.response.json();

        // Ensure that the Patient IDs haven't changed.
        pm.expect(response.member.length).to.equal(5);
        var ids = new Set(JSON.parse(pm.globals.get("patient_ids")));
        response.member.forEach(member => {
            pm.expect(ids.has(member.entity.reference)).to.be.true;
            // Ensure that the period temporals are not the same
            pm.expect(member.period.start).to.not.equal(member.period.end);
        });

        pm.globals.set("attribution_group_id", response.id);
    })
    """
    url = API_BASE + 'Group'
    headers = fhir_headers_with_attestation(org_id, provider_id)
    data = bundle('roster')
    data['member'] = [{'entity': { 'reference': f'Patient/{patient_id}' } } for patient_id in patient_ids]

    def response_test(resp, body):
        match_eq(resp.status, 201)
        match_eq(resp.headers['content-type'], FHIR_TYPE)
        roster = json.loads(body)
        patients = roster['member']
        match_eq(len(patients), 5)
        for patient in patients:
            match_truth(dig(patient, 'entity', 'reference'), 'patient entity reference')
            match_ne(dig(patient, 'period', 'start'), dig(patient, 'period', 'end'))
        return roster['id']
    return post(url, data, headers=headers, response_test=response_test)

def find_patient_by_mbi():
    """ From EndToEndRequestTest
    // Status should be 200
    pm.test("Status is 200", function () {
        pm.response.to.have.status(200);
    });

    // Response should have FHIR Content-Type
    pm.test("Content-type is application/fhir+json", function() {
       pm.response.to.have.header("Content-Type", "application/fhir+json");
    });

    pm.test("Bundle should have a single entry", function() {
        var response = pm.response.json();

        // Should be a search set with 1 entry
        pm.expect(response.type).to.equal("searchset")
        pm.expect(response.total).to.equal(1);

        pm.globals.set("single_patient_id", "Patient/" + response.entry[0].resource.id);
    })
    pm.test("Result should have MBI as uppercase", function() {
        var response = pm.response.json();
        var mbiIdentifier = response.entry[0].resource.identifier.filter(identifier => identifier.system == "http://hl7.org/fhir/sid/us-mbi")[0]
        pm.expect(mbiIdentifier.value).to.equal("1SQ3F00AA00");
    })
    """
    url = API_BASE + 'Patient?identifier=1SQ3F00AA00'
    def response_test(resp, body):
        match_fhir_ok(resp)
        patient = json.loads(body)
        match_eq(dig(patient, 'type'), 'searchset')
        match_eq(dig(patient, 'total'), 1)
        return dig(patient, 'entry', 0, 'resource', 'id')

    return get(url, headers=FHIR_HEADERS, response_test=response_test)

def find_roster_by_npi(roster_id):
    """ From EndToEndRequestTest
    // Status should be 200
    pm.test("Status is 200", function () {
        pm.response.to.have.status(200);
    });

    // Response should have FHIR Content-Type
    pm.test("Content-type is application/fhir+json", function() {
       pm.response.to.have.header("Content-Type", "application/fhir+json");
    });

    pm.test("Bundle should have a single entry", function() {
        var response = pm.response.json();

        // Should be a search set with 1 entry
        pm.expect(response.type).to.equal("searchset")
        pm.expect(response.total).to.equal(1);

        console.log("Entry:", response.entry[0]);
        console.log("Stringed: ", JSON.stringify(response));
        pm.globals.set("attribution_group", JSON.stringify(response.entry[0].resource));
    })
    """
    url = API_BASE + 'Group?characteristic-value=attributed-to$2459425221'
    def response_test(resp, body):
        match_fhir_ok(resp)
        roster = json.loads(body)
        match_eq(dig(roster, 'type'),'searchset')
        match_eq(dig(roster, 'total'), 1)
        match_eq(dig(roster, 'entry', 0, 'resource', 'id'), roster_id)

    get(url, headers=FHIR_HEADERS, response_test=response_test)

def add_patient_to_roster(org_id, roster_id, provider_id, patient_id):
    """ not in From EndToEndRequestTest, but needed for robustness """
    url = API_BASE + f'Group/{roster_id}/$add'
    headers = fhir_headers_with_attestation(org_id, provider_id)
    data = bundle('roster')
    data['member'] = [{'entity': { 'reference': f'Patient/{patient_id}' } }]
    def response_test(resp, body):
        match_fhir_ok(resp)
        members = dig(json.loads(body), 'member')
        match_truth(members, 'members')
        present = [m for m in members if dig(m, 'entity', 'reference') == f'Patient/{patient_id}']
        match_truth(present, 'added patient')

    post(url, data, headers=headers, response_test=response_test)

def remove_patient_from_roster(org_id, roster_id, provider_id, patient_id):
    """ From EndToEndRequestTest
    // Status should be 200
    pm.test("Status is 200", function () {
        pm.response.to.have.status(200);
    });

    // Response should have FHIR Content-Type
    pm.test("Content-type is application/fhir+json", function() {
       pm.response.to.have.header("Content-Type", "application/fhir+json");
    });

    pm.test("Group should have one inactive member", function() {
        var response = pm.response.json();

        // Should be a search set with one less entry
        console.debug(response.member);
        pm.expect(response.member.filter(member => member.inactive === false)).to.have.lengthOf(4);
        pm.expect(response.member.filter(member => member.inactive === true)).to.have.lengthOf(1);
    });
    """
    url = API_BASE + f'Group/{roster_id}/$remove'
    headers = fhir_headers_with_attestation(org_id, provider_id)
    data = bundle('roster')
    data['member'] = [{'entity': { 'reference': f'Patient/{patient_id}' } }]
    def response_test(resp, body):
        match_fhir_ok(resp)
        members = dig(json.loads(body), 'member')
        match_eq(len([member for member in members if member['inactive']]), 1)
        match_eq(len([member for member in members if not member['inactive']]), 4)
    post(url, data, headers=headers, response_test=response_test)

def add_unknown_patient_to_roster(org_id, roster_id, provider_id):
    """ From EndToEndRequestTest
    // Status should be 400
    pm.test("Status is 400", function () {
        pm.response.to.have.status(400);
    });

    // Response should have FHIR Content-Type
    pm.test("Content-type is application/fhir+json", function() {
       pm.response.to.have.header("Content-Type", "application/fhir+json");
    });

    pm.test("Outcome has a useful error message", function() {
        var response = pm.response.json();

        // Should be a search set with one less entry
        pm.expect(response.issue).to.have.length(1);
        pm.expect(response.issue[0].details.text).to.have.string('All patients in group must exist. Cannot find 1 patient(s).');
    });
    """
    url = API_BASE + f'Group/{roster_id}/$add'
    headers = fhir_headers_with_attestation(org_id, provider_id)
    data = bundle('roster')
    data['member'] = [{'entity': { 'reference': 'Patient/c22044f0-3b8e-488c-bcd4-fcbc630d9c19' } }]
    def error_test(e):
        match_eq(e.code, 400)
        match_eq(e.headers['content-type'], FHIR_TYPE)
        message = json.loads(e.fp.read().decode('utf-8'))
        match_eq(dig(message, 'issue', 0, 'details', 'text',), 'All patients in group must exist. Cannot find 1 patient(s).')
    post(url, data, headers=headers, error_test=error_test)

def bulk_export(roster_id):
    """ From EndToEndRequestTest
    // Status should be 202
    pm.test("Status is 202", function () {
        pm.response.to.have.status(202);
    });

    // Url for job response should be in content-location header.
    pm.test("Content-Location header is present", function () {
        pm.response.to.have.header("Content-Location");
    });

    if (pm.response.headers.get("Content-Location")) {
      pm.environment.set("content_location", pm.response.headers.get("Content-Location"));
    } else {
      // Fail the test, no content location.
      pm.execution.setNextRequest(null);
    }
    """
    url = API_BASE + f'Group/{roster_id}/$export'
    headers = async_fhir_headers()

    def response_test(resp, _):
        match_eq(resp.status, 202)
        match_truth(resp.headers['content-location'], 'content-location')
        return resp.headers['content-location']
    return get(url, headers=headers, response_test=response_test)

def job_result(org_id, url):
    """ From EndToEndRequestTest
    if (pm.response.code == 200) {
        // Response should have FHIR Content-Type
        pm.test("Content-type is application/json", function() {
           pm.response.to.have.header("Content-Type", "application/json");
        });

        // If response code is 200, check the response and load the urls.
        pm.test("Patient, EOB, and Coverage resources; one error file", function() {
            pm.expect(pm.response.json().error).to.have.lengthOf(1);
            pm.expect(pm.response.json().output).to.have.lengthOf(3);

            // Order of the output types is not guaranteed
            var outputTypes = pm.response.json().output.map((elem) => elem.type);
            pm.expect(outputTypes).to.include("Patient");
            pm.expect(outputTypes).to.include("Coverage");
            pm.expect(outputTypes).to.include("ExplanationOfBenefit");

            // Patient
            var patient = pm.response.json().output.filter((elem) => elem.type == "Patient")[0];
            console.log("patient: ", patient);
            pm.expect(patient.count).to.equal(3);
            pm.expect(Object.keys(patient.extension[0]).length).to.equal(2);
            pm.expect(patient.extension[0].url).to.equal("https://dpc.cms.gov/checksum");
            pm.expect(patient.extension[0].valueString).to.exist;
            pm.expect(Object.keys(patient.extension[1]).length).to.equal(2);
            pm.expect(patient.extension[1].url).to.equal("https://dpc.cms.gov/file_length");
            pm.expect(patient.extension[1].valueDecimal).to.exist;
            pm.environment.set("patient", patient);

            // EOB
            var eob = pm.response.json().output.filter((elem) => elem.type == "ExplanationOfBenefit")[0];
            console.log("eob: ", eob);
            pm.expect(eob.count).to.be.above(100);
            pm.expect(Object.keys(eob.extension[0]).length).to.equal(2);
            pm.expect(eob.extension[0].url).to.equal("https://dpc.cms.gov/checksum");
            pm.expect(eob.extension[0].valueString).to.exist;
            pm.expect(Object.keys(eob.extension[1]).length).to.equal(2);
            pm.expect(eob.extension[1].url).to.equal("https://dpc.cms.gov/file_length");
            pm.expect(eob.extension[1].valueDecimal).to.exist;
            pm.environment.set("eob", eob);

            // Coverage
            var coverage = pm.response.json().output.filter((elem) => elem.type == "Coverage")[0];
            pm.expect(coverage.count).to.equal(12);
            pm.expect(Object.keys(coverage.extension[0]).length).to.equal(2);
            pm.expect(coverage.extension[0].url).to.equal("https://dpc.cms.gov/checksum");
            pm.expect(coverage.extension[0].valueString).to.exist;
            pm.expect(Object.keys(coverage.extension[1]).length).to.equal(2);
            pm.expect(coverage.extension[1].url).to.equal("https://dpc.cms.gov/file_length");
            pm.expect(coverage.extension[1].valueDecimal).to.exist;
            pm.environment.set("coverage", coverage);

            // OperationOutcome
            var operationOutcome = pm.response.json().error.filter((elem) => elem.type == "OperationOutcome")[0];
            pm.expect(operationOutcome.count).to.equal(1);
            pm.expect(Object.keys(operationOutcome.extension[0]).length).to.equal(2);
            pm.expect(operationOutcome.extension[0].url).to.equal("https://dpc.cms.gov/checksum");
            pm.expect(operationOutcome.extension[0].valueString).to.exist;
            pm.expect(Object.keys(operationOutcome.extension[1]).length).to.equal(2);
            pm.expect(operationOutcome.extension[1].url).to.equal("https://dpc.cms.gov/file_length");
            pm.expect(operationOutcome.extension[1].valueDecimal).to.exist;
            pm.environment.set("operationOutcome", operationOutcome);

            var expiresHeader = pm.response.headers.get("Expires");
            pm.expect(expiresHeader).to.exist;
            var expiresInHrs = (Date.parse(expiresHeader) - Date.now())/3600000;
            pm.expect(expiresInHrs).to.be.above(23);
            pm.expect(expiresInHrs).to.be.below(24);
        });
    } else {
        // If response code is not 200, it should be 202. Assert that, and retry.
        pm.test("Status code is 202", function () {
            pm.response.to.have.status(202);
        });
        pm.execution.setNextRequest("Job response");
    }
    """
    class JobResults:
        def __init__(self, data):
            self.outputs = {}
            for output in data['output']:
                self.outputs[output['type']] = output
            match_eq(self.outputs.keys(), {'Patient', 'Coverage', 'ExplanationOfBenefit'})
            self.coverage = self.outputs['Coverage']
            self.patient = self.outputs['Patient']
            self.eob = self.outputs['ExplanationOfBenefit']
            self.operation_outcome = dig(data, 'error', 0)
        @property
        def patient_url(self):
            return self.patient['url']
        @property
        def eob_url(self):
            return self.eob['url']
        @property
        def coverage_url(self):
            return self.coverage['url']
        @property
        def operation_outcome_url(self):
            return self.operation_outcome['url']
        @property
        def patient_sha(self):
            return dig(self.patient, 'extension', 0, 'valueString')
        @property
        def coverage_sha(self):
            return dig(self.coverage, 'extension', 0, 'valueString')
        @property
        def operation_outcome_sha(self):
            return dig(self.operation_outcome, 'extension', 0, 'valueString')

    def response_test(resp, body):
        if resp.status == 202:
            time.sleep(1)
            return job_result(org_id, url)

        match_eq(resp.status, 200)
        match_eq(resp.headers['content-type'], 'application/json')

        try:
            fmt = '%a, %d %b %Y %H:%M:%S GMT'
            expires = datetime.strptime(resp.headers['expires'], fmt).replace(tzinfo=UTC)
            expires_in = expires - datetime.now(UTC)
            if not 23*60*60 < expires_in.seconds < 24*60*60:
                hours = expires_in.seconds/3600
                raise ExpectationException('Expires between 23 and 24 hours', f'Expires in {hours:.2f} hour(s)')
        except ValueError as e:
            raise ExpectationException('Expires parseable', 'Expires not parseable') from e

        data = json.loads(body)
        match_eq(len(data['error']), 1)
        match_eq(len(data['output']), 3)

        job_results = JobResults(data)

        patient = job_results.patient
        match_eq(patient['count'], 3)
        match_valid_extension(patient)

        coverage = job_results.coverage
        match_eq(coverage['count'], 12)
        match_valid_extension(coverage)

        eob = job_results.eob
        if not eob['count'] > 100:
            raise ExpectationException('eob count > 100', eob['count'])
        match_valid_extension(eob)

        operation_outcome = job_results.operation_outcome
        match_eq(operation_outcome['count'], 1)
        match_valid_extension(operation_outcome)

        return job_results
    return get(url, response_test=response_test)

def patient_data(url, sha):
    """ From EndToEndRequestTest
    // Response should have FHIR Content-Type
    pm.test("Content-type is application/ndjson", function() {
       pm.response.to.have.header("Content-Type", "application/ndjson");
    });

    // There should be 2 patients.
    pm.test("Patient data correct", function() {
        var responseOutput = pm.response.text();
        var responseLines = responseOutput.trim().split('\\n');
        pm.expect(responseLines).to.have.lengthOf(3);
        for (var line of responseLines) {
            var lineData = JSON.parse(line);
            pm.expect(lineData.resourceType).to.equal("Patient");
            var mbi = lineData.identifier.find(i => i.system === "http://hl7.org/fhir/sid/us-mbi");
            pm.expect(mbi).not.eq(undefined);
        }
    });

    // Verify sha256 checksums
    pm.test("Checksums should match", function() {
        var output = pm.environment.get("patient")
        var responseOutput = pm.response.text();

        var checksum  = CryptoJS.SHA256(responseOutput);
        pm.expect("sha256:" + checksum).to.equal(output.extension[0].valueString)
    })
    """
    def response_test(resp, body):
        match_ndjson_ok(resp)
        lines = [l for l in body.split('\n') if l]
        match_eq(len(lines), 3)
        match_sha(body, sha)
        for line in lines:
            data = json.loads(line)
            match_eq(dig(data, 'resourceType'), 'Patient')
            mbi_stanzas = [stanza for stanza in data['identifier'] if stanza['system'] == 'http://hl7.org/fhir/sid/us-mbi']
            match_eq(len(mbi_stanzas), 1)
    get(url, response_test=response_test)

def eob_data(url):
    """ From EndToEndRequestTest
    // Response should have FHIR Content-Type
    pm.test("Content-type is application/ndjson", function() {
       pm.response.to.have.header("Content-Type", "application/ndjson");
       pm.environment.set("file_timestamp", pm.response.headers.get("Last-Modified"))
    });

    // There should be 86 Explanations of Benefits.
    pm.test("Explanation of Benefits data correct", function() {
        var responseOutput = pm.response.text();
        var responseLines = responseOutput.trim().split('\\n');
        pm.expect(responseLines.length).to.be.above(100);
        for (var line of responseLines) {
            var lineData = JSON.parse(line);
            pm.expect(lineData.resourceType).to.equal("ExplanationOfBenefit");
        }
    });
    """
    def response_test(resp, body):
        match_ndjson_ok(resp)
        lines = [l for l in body.split('\n') if l]
        if not len(lines) > 100:
            raise ExpectationException('eob count > 100', len(lines))
        for line in lines:
            data = json.loads(line)
            match_eq(dig(data, 'resourceType'), 'ExplanationOfBenefit')
        return resp.headers['last-modified']
    return get(url, response_test=response_test)

def coverage_data(url, sha):
    """ From EndToEndRequestTest
    // Response should have FHIR Content-Type
    pm.test("Content-type is application/ndjson", function() {
       pm.response.to.have.header("Content-Type", "application/ndjson");
    });

    // There should be 8 coverage.
    pm.test("Coverage data correct", function() {
        var responseOutput = pm.response.text();
        var responseLines = responseOutput.trim().split('\\n');
        pm.expect(responseLines).to.have.lengthOf(12);
        for (var line of responseLines) {
            var lineData = JSON.parse(line);
            pm.expect(lineData.resourceType).to.equal("Coverage");
        }
    });

    // Verify sha256 checksum
    pm.test("Checksum should match", function() {
        var output = pm.environment.get("coverage")
        var responseOutput = pm.response.text();

        var checksum  = CryptoJS.SHA256(responseOutput);
        pm.expect("sha256:" + checksum).to.equal(output.extension[0].valueString)

    })
    """
    def response_test(resp, body):
        match_ndjson_ok(resp)
        lines = [l for l in body.split('\n') if l]
        match_eq(len(lines), 12)
        match_sha(body, sha)
        for line in lines:
            data = json.loads(line)
            match_eq(dig(data, 'resourceType'), 'Coverage')

    get(url, response_test=response_test)

def operation_outcome_data(url, sha):
    """ From EndToEndRequestTest
    // Response should have FHIR Content-Type
    pm.test("Content-type is application/ndjson", function() {
       pm.response.to.have.header("Content-Type", "application/ndjson");
    });

    // There should be 1 operation outcome.
    pm.test("OperationOutcome data correct", function() {
        var responseOutput = pm.response.text();
        var responseLines = responseOutput.trim().split('\\n');
        pm.expect(responseLines).to.have.lengthOf(1);
        var operationOutcomeItem = JSON.parse(responseLines[0]);
        pm.expect(operationOutcomeItem.resourceType).to.equal("OperationOutcome");
        pm.expect(operationOutcomeItem.issue).to.have.lengthOf(1);
        var issue = operationOutcomeItem.issue[0];
        pm.expect(issue.details.text).to.equal("Unable to retrieve patient data due to internal error");
        pm.expect(issue.location).to.include('0S80C00AA00');
    });

    // Verify sha256 checksum
    pm.test("Checksum should match", function() {
        var output = pm.environment.get("operationOutcome")
        var responseOutput = pm.response.text();

        var checksum  = CryptoJS.SHA256(responseOutput);
        pm.expect("sha256:" + checksum).to.equal(output.extension[0].valueString)

    })
    """
    def response_test(resp, body):
        match_ndjson_ok(resp)
        lines = [l for l in body.split('\n') if l]
        match_eq(len(lines), 1)
        match_sha(body, sha)
        for line in lines:
            data = json.loads(line)
            match_eq(dig(data, 'resourceType'), 'OperationOutcome')
            match_eq(len(data['issue']), 1)
            match_eq(dig(data, 'issue', 0, 'details', 'text'), 'Unable to retrieve patient data due to internal error')
            location = dig(data, 'issue', 0, 'location')
            if not '0S80C00AA00' in location:
                raise ExpectationException('0S80C00AA00 in location', location)
    get(url, response_test=response_test)

def request_partial_range(url):
    """ From EndToEndRequestTest
    // Response should have FHIR Content-Type
    pm.test("Content-type is application/ndjson", function() {
       pm.response.to.have.header("Content-Range");
    });

    // Should be 10kb total
    pm.test('Should have 10kb of data', function() {
        pm.expect(pm.response.text().length).to.be.above(10000);
    })

    pm.test('Content should be gzipped', function() {
        pm.response.to.have.header("Content-Encoding", "gzip");
    })
    """
    requested_byte_count = 10240
    headers = {'Range': f'bytes=0-{requested_byte_count}'}
    def response_test(resp, body):
        if not 'content-range' in resp.headers:
            raise ExpectationException('Content-range in headers', 'Not in headers')
        match_eq(len(body), requested_byte_count)
    get(url, headers=headers, response_test=response_test)

def request_modified_since(url, file_timestamp):
    """ From EndToEndRequestTest
    // Response should be 304
    pm.test('Response should be not-modified', function() {
        pm.response.to.have.status(304);
    })
    """
    headers = {'If-Modified-Since': file_timestamp}
    def error_test(e):
        match_eq(e.code, 304)
    get(url, headers=headers, error_test=error_test)

def bulk_export_since(roster_id):
    """ From EndToEndRequestTest
    // Status should be 202
    pm.test("Status is 202", function () {
        pm.response.to.have.status(202);
    });

    // Url for job response should be in content-location header.
    pm.test("Content-Location header is present", function () {
        pm.response.to.have.header("Content-Location");
    });

    if (pm.response.headers.get("Content-Location")) {
      pm.environment.set("since_content_location", pm.response.headers.get("Content-Location"));
    } else {
      // Fail the test, no content location.
      pm.execution.setNextRequest(null);
    }
    """
    url = API_BASE + f'Group/{roster_id}/$export?_since={datetime.now(UTC).isoformat()[:23]}Z'

    def response_test(resp, _):
        match_eq(resp.status, 202)
        match_truth(resp.headers['content-location'], 'content-location')
        return resp.headers['content-location']
    return get(url, headers=async_fhir_headers(), response_test=response_test)

def job_result_with_since(org_id, url):
    """ From EndToEndRequestTest
    if (pm.response.code == 200) {
        // Response should have FHIR Content-Type
        pm.test("Content-type is application/json", function() {
           pm.response.to.have.header("Content-Type", "application/json");
        });

        // If response code is 200, check the response and load the urls.
        pm.test("Empty output and no errors", function() {
            pm.expect(pm.response.json().error).to.have.lengthOf(0);
            pm.expect(pm.response.json().output).to.have.lengthOf(0);
        });
    } else {
        // If response code is not 200, it should be 202. Assert that, and retry.
        pm.test("Status code is 202", function () {
            pm.response.to.have.status(202);
        });
        pm.execution.setNextRequest("_since job response");
    }
    """
    def response_test(resp, body):
        if resp.status == 202:
            time.sleep(1)
            return job_result_with_since(org_id, url)
        match_eq(resp.status, 200)
        match_eq(resp.headers['content-type'], 'application/json')
        data = json.loads(body)
        match_eq(len(data['error']), 0, 'Error')
        match_eq(len(data['output']), 0, 'Output')
        return data
    return get(url, response_test=response_test)

def patient_everything(org_id, provider_id, patient_id):
    """ From EndToEndRequestTest
    pm.test("Status code should be 200", function () {
        pm.response.to.have.status(200);
    });

    pm.test("Response should be a Bundle", function() {
        var response = pm.response.json();
        pm.expect(response.resourceType).to.equal("Bundle");
        var entries = response.entry;
        var patients = entries.filter(e => e.resource.resourceType === "Patient");
        pm.expect(patients.length).to.equal(1);
        var coverage = entries.filter(e => e.resource.resourceType === "Coverage");
        pm.expect(coverage.length).to.equal(4);
        var eob = entries.filter(e => e.resource.resourceType === "ExplanationOfBenefit");
        pm.expect(eob.length).to.equal(10);
    });
    """
    url = API_BASE + f'Patient/{patient_id}/$everything'
    headers = { 'X-Provenance': attestation(org_id, provider_id) }
    def response_test(resp, body):
        match_eq(resp.status, 200)
        data = json.loads(body)
        match_eq(data['resourceType'], 'Bundle')

        resources = [entry['resource'] for entry in data['entry']]
        match_eq(len(resources), 15)

        patients = [resource for resource in resources if resource['resourceType'] == 'Patient']
        match_eq(len(patients), 1)

        coverages = [resource for resource in resources if resource['resourceType'] == 'Coverage']
        match_eq(len(coverages), 4)

        eobs = [resource for resource in resources if resource['resourceType'] == 'ExplanationOfBenefit']
        match_eq(len(eobs), 10)
    get(url, headers=headers, response_test=response_test)

def update_invalid_content_type(org_id):
    """ From EndToEndRequestTest
    pm.test("Status is 415", function () {
        pm.response.to.have.status(415);
    });

    pm.test("Body matches expected response", function() {
        var respJson = pm.response.json();
        pm.expect(respJson.issue).to.have.length(1)
        pm.expect(respJson.issue[0].details.text).to.be.equal("`Content-Type:` header must specify valid FHIR content type")
    });
    """
    url = API_BASE + f'Organization/{org_id}'
    headers = { 'Content-Type': 'application/fire+json' }
    def error_test(e):
        match_eq(e.code, 415)
        body = json.loads(e.fp.read().decode('utf-8'))
        match_eq(dig(body, 'issue', 0, 'details', 'text',), '`Content-Type:` header must specify valid FHIR content type')
    org_bundle = bundle('organization_update')
    return put(url, org_bundle, headers=headers, error_test=error_test)

def update_organization(org_id):
    """ From EndToEndRequestTest
    pm.test("Status is OK", function () {
        pm.response.to.be.ok;
    });

    pm.test("Body matches expected response", function() {
        var respJson = pm.response.json();
        pm.expect(respJson.name).equals("Beth Israel Deaconess HealthCare - Chestnut Hill");

        var address = respJson.address[0];
        pm.expect(address.line[0]).equals("200 Boylston Street, 4th Floor");
        pm.expect(address.city).equals("Chestnut Hill");
        pm.expect(address.state).equals("MA");
        pm.expect(address.postalCode).equals("02467");
    });
    """
    url = API_BASE + f'Organization/{org_id}'
    org_bundle = bundle('organization_update')
    def response_test(resp, body):
        match_fhir_ok(resp)
        data = json.loads(body)
        match_eq(data['name'], org_bundle['name'])
        match_eq(data['address'], org_bundle['address'])
    return put(url, org_bundle, headers=FHIR_HEADERS, response_test=response_test)

def find_practitioner_by_npi():
    """ From EndToEndRequestTest
    // Status should be 200
    pm.test("Status is 200", function () {
        pm.response.to.have.status(200);
    });

    // Response should have FHIR Content-Type
    pm.test("Content-type is application/fhir+json", function() {
       pm.response.to.have.header("Content-Type", "application/fhir+json");
    });

    pm.test("Bundle should have a single entry", function() {
        var response = pm.response.json();

        // Should be a search set with 1 entry
        pm.expect(response.type).to.equal("searchset")
        pm.expect(response.total).to.equal(1);

        pm.globals.set("single_practitioner_id", response.entry[0].resource.id);
    })
    """
    url = API_BASE + 'Practitioner?identifier=2459425221'
    def response_test(resp, body):
        match_fhir_ok(resp)
        data = json.loads(body)
        match_eq(data['type'], 'searchset')
        match_eq(data['total'], 1)
        provider_id = dig(data, 'entry', 0, 'resource', 'id')
        match_truth(provider_id, 'provider id')
        return provider_id
    return get(url, response_test=response_test)

def patient_missing_after_delete(patient_id, patient_ids, roster_id):
    """ From EndToEndRequestTest
    // Status should be 200
    pm.test("Status is 200", function () {
        pm.response.to.have.status(200);
    });

    // Response should have FHIR Content-Type
    pm.test("Content-type is application/fhir+json", function() {
       pm.response.to.have.header("Content-Type", "application/fhir+json");
    });

    pm.test("Response Body is correct", () => {
        console.log("Body correct?");
        const patient_id = pm.globals.get("single_patient_id")
        // Check to ensure that each member id matches what we previously had
        var response = pm.response.json();

        // Ensure that the Patient IDs is less than the original submission number
        pm.expect(response.member.length).to.equal(4);
        var ids = new Set(JSON.parse(pm.globals.get("patient_ids")));
        const matchingPatient = response.member.filter(member => {
            pm.expect(ids.has(member.entity.reference)).to.be.true;
            return member.entity.referenced === patient_id
        });
        pm.expect(matchingPatient).to.be.empty;

        pm.globals.set("attribution_group_id", response.id);
    })

    """
    # Note: there is no test for deletion
    delete(API_BASE + f'Patient/{patient_id}')

    def response_test(resp, body):
        match_fhir_ok(resp)
        data = json.loads(body)
        match_eq(len(data['member']), len(patient_ids) - 1)
        for existing_patient in data['member']:
            existing_id = dig(existing_patient, 'entity', 'reference').replace('Patient/', '')
            if not existing_id in patient_ids:
                raise ExpectationException(f'{existing_id} in patients', 'was not')
            match_ne(existing_id, patient_id)
    url = API_BASE + f'Group/{roster_id}'
    get(url, response_test=response_test)

def roster_missing_after_practitioner_delete(practitioner_id):
    """ From EndToEndRequestTest
    pm.test("Bundle should be empty", function() {
        var response = pm.response.json();

        // Should be a search set with 0 entries
        pm.expect(response.type).to.equal("searchset")
        pm.expect(response.total).to.equal(0);
    })
    """
    # Note: there is no test for deletion
    delete(API_BASE + f'Practitioner/{practitioner_id}')

    url = API_BASE + 'Group?characteristic-value=attributed-to$2459425221'
    def response_test(resp, body):
        match_eq(resp.status, 200)
        data = json.loads(body)
        match_eq(data['type'], 'searchset')
        match_eq(data['total'], 0)
    get(url, response_test=response_test)

class TestRunner:
    def __init__(self):
        self.success_count = 0
        self.failures = []
    def _success_msg(self, msg):
        print(f'\033[92m {msg}\033[00m')

    def _fail_msg(self, msg):
        print(f'\033[91m {msg}\033[00m')
    def run_test(self, name, function, *args):
        try:
            result = function(*args)
            self._success_msg(f'{name} success')
            self.success_count += 1
            return result
        except ExpectationException as e:
            self.fail_msg(f'{name} failure')
            self.failures.append((name, e,))
            self._fail_msg(f'  {e}')
            return None
    def finish(self):
        print(f'{self.success_count} SUCCESSFUL TESTS')
        if self.failures:
            failure_msg = 'FAILURE' if len(self.failures) == 1 else 'FAILURES'
            print('XXXXXXXXXXXXXXXXXXXX')
            print(f'{len(self.failures)} {failure_msg}')
            print('XXXXXXXXXXXXXXXXXXXX')
            for name, exception in self.failures:
                print(f'{name:40}: {exception}')
            sys.exit(1)

def export_tests(tr, org_id, roster_id, provider_id, patient_id):
    tr.run_test('Find roster by npi', find_roster_by_npi, roster_id)

    if patient_id:
        tr.run_test('Add patient to roster', add_patient_to_roster, org_id, roster_id, provider_id, patient_id)
        tr.run_test('Remove patient from roster', remove_patient_from_roster, org_id, roster_id, provider_id, patient_id)
        tr.run_test('Add unknown patient to roster', add_unknown_patient_to_roster, org_id, roster_id, provider_id)

    location = tr.run_test('Bulk export', bulk_export, roster_id)
    if location:
        job_results = tr.run_test('Job result', job_result, org_id, location)
        if job_results:
            tr.run_test('Patient data', patient_data, job_results.patient_url, job_results.patient_sha)

            last_modified = tr.run_test('Eob data', eob_data, job_results.eob_url)
            if last_modified:
                tr.run_test('Request partial range', request_partial_range, job_results.eob_url)
                tr.run_test('Request modified since', request_modified_since, job_results.eob_url, last_modified)

            tr.run_test('Coverage data', coverage_data, job_results.coverage_url, job_results.coverage_sha)

            tr.run_test('Operation outcome data', operation_outcome_data, job_results.operation_outcome_url,
                        job_results.operation_outcome_sha)

    since_location = tr.run_test('Bulk export with since', bulk_export_since, roster_id)
    if since_location:
        tr.run_test('Job result with since', job_result_with_since, org_id, since_location)

def run():
    """
    On failure, it might be necessary to set the roster id to that which is
    printed to the command line.
    """
    tr = TestRunner()
    org_id = tr.run_test('Create Organization', create_organization)
    if not org_id:
        tr.finish()

    try:
        provider_id = tr.run_test('Register providers', register_providers)[0]
    except TypeError:
        provider_id = None

    patient_ids = tr.run_test('Register patients', register_patients)

    if provider_id and patient_ids:
        # check for roster id from previous run before running test
        roster_id = check_for_roster() or tr.run_test('Submit roster', submit_roster, org_id, provider_id, patient_ids)
    else:
        roster_id = None

    if patient_ids:
        patient_id = tr.run_test('Find patient by mbi', find_patient_by_mbi)
    else:
        patient_id = None

    if roster_id:
        export_tests(tr, org_id, roster_id, provider_id, patient_id)

    if patient_id and provider_id:
        tr.run_test('Patient everything', patient_everything, org_id, provider_id, patient_id)

    tr.run_test('Update invalid content type', update_invalid_content_type, org_id)

    tr.run_test('Update organization', update_organization, org_id)

    if provider_id:
        tr.run_test('Find practitioner by npi', find_practitioner_by_npi, )

    if patient_id and roster_id:
        tr.run_test('Patient missing after delete', patient_missing_after_delete, patient_id, patient_ids, roster_id)

    if provider_id and roster_id:
        tr.run_test('Roster missing after practitioner delete', roster_missing_after_practitioner_delete, provider_id)

    tr.finish()

if __name__ == '__main__':
    run()
