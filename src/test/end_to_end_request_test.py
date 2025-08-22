#!/usr/bin/env python
"""
Runs end-to-end test of API
"""

from datetime import datetime, UTC
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
    def __init__(self, expected, actual):
        self.expected = expected
        self.actual = actual
        super().__init__(f'Expected {expected} | Actual {actual}')

def dig(_dict, *keys):
    try:
        for key in keys:
            _dict = _dict[key]
        return _dict
    except (KeyError, IndexError):
        return None

def fhir_headers(org_id=None, attestation=None):
    headers = FHIR_HEADERS.copy()
    if org_id:
        headers['Organization'] = org_id
    if attestation:
        headers['X-Provenance'] = json.dumps({ "resourceType":"Provenance",
             "meta":{ "profile":[ "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-attestation" ] },
             "recorded": datetime.now(UTC).isoformat(),
             "reason":[ { "system":"http://hl7.org/fhir/v3/ActReason", "code":"TREAT" } ],
             "agent":[ { "role":[ { "coding":[ { "system":"http://hl7.org/fhir/v3/RoleClass", "code":"AGNT" } ] } ],
                         "whoReference":{ "reference":f"Organization/{org_id}" },
                         "onBehalfOfReference":{ "reference":f"Practitioner/{attestation}" } } ] })

    return headers

def get(url, headers, response_test):
    req = request.Request(url)
    for key, value in headers.items():
        req.add_header(key, value)
    try:
        with request.urlopen(req) as resp:
            body = resp.read().decode('utf-8')
            return response_test(resp, body)
    except URLError as e:
        raise ExpectationException('No error', e)

def delete(url, headers):
    req = request.Request(url, method='DELETE')
    for key, value in headers.items():
        req.add_header(key, value)
    try:
        with request.urlopen(req) as resp:
            return resp
    except URLError as e:
        raise ExpectationException('No error', e)

def post(url, headers, message, response_test, error_test=None):
    jsondata = json.dumps(message)
    jsondataasbytes = jsondata.encode('utf-8')
    req = request.Request(url)
    for key, value in headers.items():
        req.add_header(key, value)
    try:
        with request.urlopen(req, jsondataasbytes) as resp:
            body = resp.read().decode('utf-8')
            return response_test(resp, body)
    except URLError as e:
        if error_test:
            error_test(e)
        else:
            raise ExpectationException('No error', e)

def bundle(name):
    with open(f'{WORKING_DIR}/bundles/{name}_bundle.json') as f:
        return json.load(f)
def basic_test(resp, _):
    match_eq(resp.status, 200)
    match_eq(resp.headers['content-type'], FHIR_TYPE)

def match_eq(actual, expect):
    if actual != expect:
        raise ExpectationException(expect, actual)

def match_ne(actual, expect):
    if actual == expect:
        raise ExpectationException(f'{actual} != {expect}', 'equality')

def create_organization():
    url = API_BASE + 'Organization/$submit'
    org_bundle = bundle('organization')
    def response_test(resp, body):
        basic_test(resp, body)
        org = json.loads(body)
        return org['id']
    return post(url, FHIR_HEADERS, org_bundle, response_test)

def register_providers(org_id):
    errors = []
    url = API_BASE + 'Practitioner/$submit'
    providers_bundle = bundle('providers')
    def response_test(resp, body):
        basic_test(resp, body)
        providers = json.loads(body)
        return [entry['resource']['id'] for entry in providers['entry']]
    return post(url, fhir_headers(org_id), providers_bundle, response_test)

def register_patients(org_id):
    url = API_BASE + 'Patient/$submit'
    patients_bundle = bundle('patients')
    def response_test(resp, body):
        basic_test(resp, body)
        patients = json.loads(body)
        match_eq(len(dig(patients, 'entry') or []), 5)
        return [ dig(patient, 'resource', 'id') for patient in dig(patients, 'entry')]
    return post(url, fhir_headers(org_id), patients_bundle, response_test)

def submit_roster(org_id, provider_id, patient_ids):
    url = API_BASE + 'Group'
    headers = fhir_headers(org_id, provider_id)
    data = bundle('roster')
    data['member'] = [{'entity': { 'reference': f'Patient/{patient_id}' } } for patient_id in patient_ids]

    def response_test(resp, body):
        match_eq(resp.status, 201)
        match_eq(resp.headers['content-type'], FHIR_TYPE)
        roster = json.loads(body)
        returned_patients = [member for member in roster['member']]
        match_eq(len(returned_patients), 5)
        for patient in returned_patients:
            match_ne(dig(patient, 'entity', 'reference'), None)
            match_ne(dig(patient, 'period', 'start'), dig(patient, 'period', 'end'))
        return roster['id']
    return post(url, headers, data, response_test)

def find_patient_by_mbi(org_id):
    url = API_BASE + 'Patient?identifier=1SQ3F00AA00'
    def response_test(resp, body):
        basic_test(resp, body)
        patient = json.loads(body)
        match_eq(dig(patient, 'type'),'searchset')
        match_eq(dig(patient, 'total'), 1)
        return dig(patient, 'entry', 0, 'resource', 'id')
        
    return get(url, fhir_headers(org_id), response_test)

def find_roster_by_npi(org_id, roster_id):
    url = API_BASE + 'Group?characteristic-value=attributed-to$2459425221'
    def response_test(resp, body):
        basic_test(resp, body)
        roster = json.loads(body)
        match_eq(dig(roster, 'type'),'searchset')
        match_eq(dig(roster, 'total'), 1)
        match_eq(dig(roster, 'entry', 0, 'resource', 'id'), roster_id)
        
    get(url, fhir_headers(org_id), response_test)

def remove_patient_from_roster(org_id, roster_id, provider_id, patient_id):
    url = API_BASE + f'Group/{roster_id}/$remove'
    headers = fhir_headers(org_id, provider_id)
    data = bundle('roster')
    data['member'] = [{'entity': { 'reference': f'Patient/{patient_id}' } }]
    def response_test(resp, body):
        basic_test(resp, body)
        members = dig(json.loads(body), 'member')
        match_eq(len([member for member in members if member['inactive']]), 1)
        match_eq(len([member for member in members if not member['inactive']]), 4)
    post(url, headers, data, response_test)

def add_unknown_patient_to_roster(org_id, roster_id, provider_id):
    url = API_BASE + f'Group/{roster_id}/$add'
    headers = fhir_headers(org_id, provider_id)
    data = bundle('roster')
    data['member'] = [{'entity': { 'reference': f'Patient/c22044f0-3b8e-488c-bcd4-fcbc630d9c19' } }]
    def error_test(e):
        match_eq(e.code, 400)
        match_eq(e.headers['content-type'], FHIR_TYPE)
        message = json.loads(e.fp.read().decode('utf-8'))
        match_eq(dig(message, 'issue', 0, 'details', 'text',), 'All patients in group must exist. Cannot find 1 patient(s).')
        
    post(url, headers, data, None, error_test)

def bulk_export(org_id, roster_id):
    url = API_BASE + f'Group/{roster_id}/$export'
    headers = fhir_headers(org_id)
    headers['Prefer'] = 'respond-async'
    def response_test(resp, body):
        match_eq(resp.status, 202)
        match_ne(resp.headers['content-location'], None)
        return resp.headers['content-location']
    return get(url, headers, response_test)

def job_result(org_id, url):
    headers = {}
    def response_test(resp, body):
        if resp.status == 202:
            time.sleep(1)
            return job_result(org_id, url)
        match_eq(resp.status, 200)
        match_eq(resp.headers['content-type'], 'application/json')
        try:
            fmt = '%a, %d %b %Y %H:%M:%S GMT'
            expires = datetime.strptime(resp.headers['expires'], fmt)
            expires = expires.replace(tzinfo=UTC)
            expires_in = expires - datetime.now(UTC)
            if not 23*60*60 < expires_in.seconds < 24*60*60:
                hours = expires_in.seconds/3600
                raise ExpectationException('Expires between 23 and 24 hours', f'Expires in {hours:.2f} hour(s)')
        except ValueError:
            raise ExpectationException('Expires parseable', 'Expires not parseable')
        data = json.loads(body)
        match_eq(len(data['error']), 1)
        match_eq(len(data['output']), 3)
        outputs = {}
        for output in data['output']:
            outputs[output['type']] = output
        match_eq(outputs.keys(), {'Patient', 'Coverage', 'ExplanationOfBenefit'})
        patient = outputs['Patient']
        match_eq(patient['count'], 3)
        match_eq(len(patient['extension']), 2)
        match_eq(len(dig(patient, 'extension', 0)), 2)
        match_eq(dig(patient, 'extension', 0, 'url'), 'https://dpc.cms.gov/checksum')
        match_ne(dig(patient, 'extension', 0, 'valueString'), None)
        match_eq(len(dig(patient, 'extension', 1)), 2)
        match_eq(dig(patient, 'extension', 1, 'url'), 'https://dpc.cms.gov/file_length')
        match_ne(dig(patient, 'extension', 1, 'valueDecimal'), None)
        coverage = outputs['Coverage']
        match_eq(coverage['count'], 12)
        match_eq(len(coverage['extension']), 2)
        match_eq(len(dig(coverage, 'extension', 0)), 2)
        match_eq(dig(coverage, 'extension', 0, 'url'), 'https://dpc.cms.gov/checksum')
        match_ne(dig(coverage, 'extension', 0, 'valueString'), None)
        match_eq(len(dig(coverage, 'extension', 1)), 2)
        match_eq(dig(coverage, 'extension', 1, 'url'), 'https://dpc.cms.gov/file_length')
        match_ne(dig(coverage, 'extension', 1, 'valueDecimal'), None)
        eob = outputs['ExplanationOfBenefit']
        if not eob['count'] > 100:
            raise ExpectationException('eob count > 100', eob['count'])
        match_eq(len(eob['extension']), 2)
        match_eq(len(dig(eob, 'extension', 0)), 2)
        match_eq(dig(eob, 'extension', 0, 'url'), 'https://dpc.cms.gov/checksum')
        match_ne(dig(eob, 'extension', 0, 'valueString'), None)
        match_eq(len(dig(eob, 'extension', 1)), 2)
        match_eq(dig(eob, 'extension', 1, 'url'), 'https://dpc.cms.gov/file_length')
        match_ne(dig(eob, 'extension', 1, 'valueDecimal'), None)
        operationOutcome = dig(data, 'error', 0)
        match_eq(operationOutcome['count'], 1)
        match_eq(len(operationOutcome['extension']), 2)
        match_eq(len(dig(operationOutcome, 'extension', 0)), 2)
        match_eq(dig(operationOutcome, 'extension', 0, 'url'), 'https://dpc.cms.gov/checksum')
        match_ne(dig(operationOutcome, 'extension', 0, 'valueString'), None)
        match_eq(len(dig(operationOutcome, 'extension', 1)), 2)
        match_eq(dig(operationOutcome, 'extension', 1, 'url'), 'https://dpc.cms.gov/file_length')
        match_ne(dig(operationOutcome, 'extension', 1, 'valueDecimal'), None)
        return patient['url'], eob['url'], coverage['url'], operationOutcome['url']
    return get(url, headers, response_test)

def patient_data(org_id, url):
    def response_test(resp, body):
        match_eq(resp.status, 200)
        match_eq(resp.headers['content-type'], 'application/ndjson')
        lines = [l for l in body.split('\n') if l]
        match_eq(len(lines), 3)
        for line in lines:
            data = json.loads(line)
            match_eq(dig(data, 'resourceType'), 'Patient')
            mbi_stanzas = [stanza for stanza in data['identifier'] if stanza['system'] == 'http://hl7.org/fhir/sid/us-mbi']
            match_eq(len(mbi_stanzas), 1)
    get(url, {}, response_test)
    
def eob_data(org_id, url):
    def response_test(resp, body):
        match_eq(resp.status, 200)
        match_eq(resp.headers['content-type'], 'application/ndjson')
        lines = [l for l in body.split('\n') if l]
        if not len(lines) > 100:
            raise ExpectationException('eob count > 100', len(lines))
        for line in lines:
            data = json.loads(line)
            match_eq(dig(data, 'resourceType'), 'ExplanationOfBenefit')
        return resp.headers['last-modified']
    return get(url, {}, response_test)
    
def coverage_data(org_id, url):
    def response_test(resp, body):
        match_eq(resp.status, 200)
        match_eq(resp.headers['content-type'], 'application/ndjson')
        lines = [l for l in body.split('\n') if l]
        match_eq(len(lines), 12)
        for line in lines:
            data = json.loads(line)
            match_eq(dig(data, 'resourceType'), 'Coverage')

    get(url, {}, response_test)
    
def operation_outcome_data(org_id, url):
    def response_test(resp, body):
        match_eq(resp.status, 200)
        match_eq(resp.headers['content-type'], 'application/ndjson')
        lines = [l for l in body.split('\n') if l]
        match_eq(len(lines), 1)
        for line in lines:
            data = json.loads(line)
            match_eq(dig(data, 'resourceType'), 'OperationOutcome')
            match_eq(len(data['issue']), 1)
            match_eq(dig(data, 'issue', 0, 'details', 'text'), 'Unable to retrieve patient data due to internal error')
            location = dig(data, 'issue', 0, 'location')
            if not '0S80C00AA00' in location:
                raise ExpectationException('0S80C00AA00 in location', location)
    get(url, {}, response_test)
    
def run():
    roster_id = '6ddb51c7-acd1-420e-85c9-6303c92aa8d1'
    try:
        org_id = create_organization()
        print('Successfully created organization')
        print(f"    org_id = '{org_id}'")
    except ExpectationException as e:
        print('Failed to create organization')
        print(f'  {e}')
        sys.exit(1)
    try: 
        provider_ids = register_providers(org_id)
        providers = True
        print('Successfully registered providers')
        print(f"    provider_ids = {provider_ids}")
    except ExpectationException as e:
        providers = False
        print('Failed to register providers')
        print(f'  {e}')
    try: 
        patient_ids = register_patients(org_id)
        patients = True
        print('Successfully registered patients')
        print(f"    patient_ids = {patient_ids}")
    except ExpectationException as e:
        patients = False
        print('Failed to register patients')
        print(f'  {e}')
    if providers and patients and not roster_id:
        try:
            roster_id = submit_roster(org_id, provider_ids[0], patient_ids)
            print('Successfully submitted roster')
            print(f"    roster_id = '{roster_id}'")
        except ExpectationException as e:
            roster_id = False
            print('Failed to submit roster')
            print(f'  {e}')
    else:
        print('Skipping roster submission')
    if patients:
        try:
            patient_id = find_patient_by_mbi(org_id)
            print('Successfully found patient by mbi')
        except ExpectationException as e:
            patient_id = False
            print('Failed to find patient by mbi')
            print(f'  {e}')
    if roster_id:
        try:
            find_roster_by_npi(org_id, roster_id)
            print('Successfully found roster by npi')
        except ExpectationException as e:
            print('Failed to find roster by npi')
            print(f'  {e}')
        if patient_id:
            try:
                remove_patient_from_roster(org_id, roster_id, provider_ids[0], patient_id)
                print('Successfully removed patient')
            except ExpectationException as e:
                print('Failed to remove patient')
                print(f'  {e}')
        try:
            add_unknown_patient_to_roster(org_id, roster_id, provider_ids[0])
            print('Successfully tested adding unknown patient')
        except ExpectationException as e:
            print('Failed to test adding unknown patient')
            print(f'  {e}')
        try:
            location = bulk_export(org_id, roster_id)
            print('Successfully bulk exported')
        except ExpectationException as e:
            location = False
            print('Failed to bulk export')
            print(f'  {e}')
        if location:
            try:
                patient_url, eob_url, coverage_url, operation_outcome_url = job_result(org_id, location)
                print('Successful job report')
            except ExpectationException as e:
                patient_url = eob_url = coverage_url = operation_outcome_url = None
                print('Failed job report')
                print(f'  {e}')
            if patient_url:
                try:
                    patient_data(org_id, patient_url)
                    print('Successfully retrieved patient data')
                except ExpectationException as e:
                    print('Failed to retrieve patient data')
                    print(f'  {e}')
            if eob_url:
                try:
                    last_modified = eob_data(org_id, eob_url)
                    print('Successfully retrieved eob data')
                except ExpectationException as e:
                    last_modified = False
                    print('Failed to retrieve eob data')
                    print(f'  {e}')
            if coverage_url:
                try:
                    coverage_data(org_id, coverage_url)
                    print('Successfully retrieved coverage data')
                except ExpectationException as e:
                    print('Failed to retrieve coverage data')
                    print(f'  {e}')
            if operation_outcome_url:
                try:
                    operation_outcome_data(org_id, operation_outcome_url)
                    print('Successfully retrieved operation outcome data')
                except ExpectationException as e:
                    print('Failed to retrieve operation outcome data')
                    print(f'  {e}')
            
if __name__ == '__main__':
    run()
