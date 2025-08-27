#!/usr/bin/env python
"""
Runs end-to-end test of API
"""

from datetime import datetime, UTC
import hashlib
import json
import sys
import time
from urllib import request
from urllib.error import URLError

import pathlib
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

def get(url, headers, response_test, error_test=None):
    req = request.Request(url)
    for key, value in headers.items():
        req.add_header(key, value)
    try:
        with request.urlopen(req) as resp:
            body = resp.read().decode('utf-8')
            if response_test:
                return response_test(resp, body)
    except URLError as e:
        if error_test:
            return error_test(e)
        raise ExpectationException('No error', e) from e
    return None

def delete(url):
    req = request.Request(url, method='DELETE')
    try:
        with request.urlopen(req) as resp:
            return resp
    except URLError as e:
        raise ExpectationException('No error', e) from e
    return None

def post(url, headers, message, response_test, error_test=None, method='POST'):
    jsondata = json.dumps(message)
    jsondataasbytes = jsondata.encode('utf-8')
    req = request.Request(url, method=method)
    for key, value in headers.items():
        req.add_header(key, value)
    try:
        with request.urlopen(req, jsondataasbytes) as resp:
            body = resp.read().decode('utf-8')
            return response_test(resp, body)
    except URLError as e:
        if error_test:
            return error_test(e)
        raise ExpectationException('No error', e) from e
    return None

def bundle(name):
    with open(f'{WORKING_DIR}/bundles/{name}_bundle.json') as f:
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

    return get(url, FHIR_HEADERS, response_test)


# TESTS
def create_organization():
    url = API_BASE + 'Organization/$submit'
    org_bundle = bundle('organization')
    def response_test(resp, body):
        match_fhir_ok(resp)
        org = json.loads(body)
        return org['id']
    return post(url, FHIR_HEADERS, org_bundle, response_test)

def register_providers():
    url = API_BASE + 'Practitioner/$submit'
    providers_bundle = bundle('providers')
    def response_test(resp, body):
        match_fhir_ok(resp)
        providers = json.loads(body)
        return [entry['resource']['id'] for entry in providers['entry']]
    return post(url, FHIR_HEADERS, providers_bundle, response_test)

def register_patients():
    url = API_BASE + 'Patient/$submit'
    patients_bundle = bundle('patients')
    def response_test(resp, body):
        match_fhir_ok(resp)
        patients = json.loads(body)
        match_eq(len(dig(patients, 'entry') or []), 5)
        return [dig(patient, 'resource', 'id') for patient in dig(patients, 'entry')]
    return post(url, FHIR_HEADERS, patients_bundle, response_test)

def submit_roster(org_id, provider_id, patient_ids):
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
    return post(url, headers, data, response_test)

def find_patient_by_mbi():
    url = API_BASE + 'Patient?identifier=1SQ3F00AA00'
    def response_test(resp, body):
        match_fhir_ok(resp)
        patient = json.loads(body)
        match_eq(dig(patient, 'type'), 'searchset')
        match_eq(dig(patient, 'total'), 1)
        return dig(patient, 'entry', 0, 'resource', 'id')

    return get(url, FHIR_HEADERS, response_test)

def find_roster_by_npi(roster_id):
    url = API_BASE + 'Group?characteristic-value=attributed-to$2459425221'
    def response_test(resp, body):
        match_fhir_ok(resp)
        roster = json.loads(body)
        match_eq(dig(roster, 'type'),'searchset')
        match_eq(dig(roster, 'total'), 1)
        match_eq(dig(roster, 'entry', 0, 'resource', 'id'), roster_id)

    get(url, FHIR_HEADERS, response_test)

def add_patient_to_roster(org_id, roster_id, provider_id, patient_id):
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

    post(url, headers, data, response_test)

def remove_patient_from_roster(org_id, roster_id, provider_id, patient_id):
    url = API_BASE + f'Group/{roster_id}/$remove'
    headers = fhir_headers_with_attestation(org_id, provider_id)
    data = bundle('roster')
    data['member'] = [{'entity': { 'reference': f'Patient/{patient_id}' } }]
    def response_test(resp, body):
        match_fhir_ok(resp)
        members = dig(json.loads(body), 'member')
        match_eq(len([member for member in members if member['inactive']]), 1)
        match_eq(len([member for member in members if not member['inactive']]), 4)
    post(url, headers, data, response_test)

def add_unknown_patient_to_roster(org_id, roster_id, provider_id):
    url = API_BASE + f'Group/{roster_id}/$add'
    headers = fhir_headers_with_attestation(org_id, provider_id)
    data = bundle('roster')
    data['member'] = [{'entity': { 'reference': 'Patient/c22044f0-3b8e-488c-bcd4-fcbc630d9c19' } }]
    def error_test(e):
        match_eq(e.code, 400)
        match_eq(e.headers['content-type'], FHIR_TYPE)
        message = json.loads(e.fp.read().decode('utf-8'))
        match_eq(dig(message, 'issue', 0, 'details', 'text',), 'All patients in group must exist. Cannot find 1 patient(s).')
    post(url, headers, data, None, error_test)

def bulk_export(roster_id):
    url = API_BASE + f'Group/{roster_id}/$export'
    headers = async_fhir_headers()

    def response_test(resp, _):
        match_eq(resp.status, 202)
        match_truth(resp.headers['content-location'], 'content-location')
        return resp.headers['content-location']
    return get(url, headers, response_test)

def job_result(org_id, url):
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

    headers = {}
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
    return get(url, headers, response_test)

def patient_data(url, sha):
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
    get(url, {}, response_test)

def eob_data(url):
    def response_test(resp, body):
        match_ndjson_ok(resp)
        lines = [l for l in body.split('\n') if l]
        if not len(lines) > 100:
            raise ExpectationException('eob count > 100', len(lines))
        for line in lines:
            data = json.loads(line)
            match_eq(dig(data, 'resourceType'), 'ExplanationOfBenefit')
        return resp.headers['last-modified']
    return get(url, {}, response_test)

def coverage_data(url, sha):
    def response_test(resp, body):
        match_ndjson_ok(resp)
        lines = [l for l in body.split('\n') if l]
        match_eq(len(lines), 12)
        match_sha(body, sha)
        for line in lines:
            data = json.loads(line)
            match_eq(dig(data, 'resourceType'), 'Coverage')

    get(url, {}, response_test)

def operation_outcome_data(url, sha):
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
    get(url, {}, response_test)

def request_partial_range(url):
    requested_byte_count = 10240
    def response_test(resp, body):
        if not 'content-range' in resp.headers:
            raise ExpectationException('Content-range in headers', 'Not in headers')
        match_eq(len(body), requested_byte_count)
    get(url, {'Range': f'bytes=0-{requested_byte_count}'}, response_test)

def request_modified_since(url, file_timestamp):
    def error_test(e):
        match_eq(e.code, 304)
    get(url, {'If-Modified-Since': file_timestamp}, None, error_test)

def bulk_export_since(roster_id):
    url = API_BASE + f'Group/{roster_id}/$export?_since={datetime.now(UTC).isoformat()[:23]}Z'

    def response_test(resp, _):
        match_eq(resp.status, 202)
        match_truth(resp.headers['content-location'], 'content-location')
        return resp.headers['content-location']
    return get(url, async_fhir_headers(), response_test)

def job_result_with_since(org_id, url):
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
    return get(url, {}, response_test)

def patient_everything(org_id, provider_id, patient_id):
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
    get(url, headers, response_test)

def update_invalid_content_type(org_id):
    url = API_BASE + f'Organization/{org_id}'
    headers = { 'Content-Type': 'application/fire+json' }
    def error_test(e):
        match_eq(e.code, 415)
        body = json.loads(e.fp.read().decode('utf-8'))
        match_eq(dig(body, 'issue', 0, 'details', 'text',), '`Content-Type:` header must specify valid FHIR content type')
    org_bundle = bundle('organization_update')
    return post(url, headers, org_bundle, None, error_test, 'PUT')

def update_organization(org_id):
    url = API_BASE + f'Organization/{org_id}'
    org_bundle = bundle('organization_update')
    def response_test(resp, body):
        match_fhir_ok(resp)
        data = json.loads(body)
        match_eq(data['name'], org_bundle['name'])
        match_eq(data['address'], org_bundle['address'])
    return post(url, FHIR_HEADERS, org_bundle, response_test, None, 'PUT')

def find_practitioner_by_npi():
    url = API_BASE + 'Practitioner?identifier=2459425221'
    def response_test(resp, body):
        match_fhir_ok(resp)
        data = json.loads(body)
        match_eq(data['type'], 'searchset')
        match_eq(data['total'], 1)
        provider_id = dig(data, 'entry', 0, 'resource', 'id')
        match_truth(provider_id, 'provider id')
        return provider_id
    return get(url, {}, response_test)

def patient_missing_after_delete(patient_id, patient_ids, roster_id):
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
    get(url, {}, response_test)

def roster_missing_after_practitioner_delete(practitioner_id):
    delete(API_BASE + f'Practitioner/{practitioner_id}')

    url = API_BASE + 'Group?characteristic-value=attributed-to$2459425221'
    def response_test(resp, body):
        match_eq(resp.status, 200)
        data = json.loads(body)
        match_eq(data['type'], 'searchset')
        match_eq(data['total'], 0)
    get(url, {}, response_test)

class TestRunner:
    def __init__(self):
        self.success_count = 0
        self.failures = []
    def run_test(self, name, function, *args):
        try:
            result = function(*args)
            print(f'{name} success')
            self.success_count += 1
            return result
        except ExpectationException as e:
            print(f'{name} failure')
            self.failures.append((name, e,))
            print(f'  {e}')
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

                tr.run_test('Operation outcome data', operation_outcome_data, job_results.operation_outcome_url, job_results.operation_outcome_sha)

        since_location = tr.run_test('Bulk export with since', bulk_export_since, roster_id)
        if since_location:
            tr.run_test('Job result with since', job_result_with_since, org_id, since_location)

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
