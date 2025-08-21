#!/usr/bin/env python
"""
Runs end-to-end test of API
"""

from datetime import datetime, UTC
import json
from urllib import request
from urllib.error import URLError

API_BASE = 'http://localhost:3002/api/v1/'
ORG_BUNDLE = {'resourceType': 'Parameters',
 'parameter': [{'name': 'resource',
   'resource': {'resourceType': 'Bundle',
    'type': 'collection',
    'entry': [{'resource': {'address': [{'use': 'work',
         'type': 'both',
         'city': 'Akron',
         'country': 'US',
         'line': ['750 Elm Street', 'Suite 66'],
         'postalCode': '22222',
         'state': 'OH'}],
       'identifier': [{'system': 'http://hl7.org/fhir/sid/us-npi',
         'value': '3609811336'}],
       'name': 'whatver',
       'resourceType': 'Organization',
       'type': [{'coding': [{'code': 'prov',
           'display': 'Healthcare Provider',
           'system': 'http://hl7.org/fhir/organization-type'}],
         'text': 'Healthcare Provider'}]}}]}}]}
PROVIDERS_BUNDLE = {'resourceType': 'Parameters',
 'parameter': [{'name': 'resource',
   'resource': {'resourceType': 'Bundle',
    'type': 'collection',
    'entry': [{'resource': {'active': True,
       'address': [{'city': 'PLYMOUTH',
         'country': 'US',
         'line': ['275 SANDWICH STREET'],
         'postalCode': '02360',
         'state': 'MA'}],
       'gender': 'male',
       'identifier': [{'system': 'http://hl7.org/fhir/sid/us-npi',
         'value': '2459425221'}],
       'name': [{'family': 'Klocko335',
         'given': ['Leonard963'],
         'prefix': ['Dr.']}],
       'resourceType': 'Practitioner'}}]}}]}
PATIENT_TEMPLATE = """
    {{
        "birthDate": "1972-11-11",
        "gender": "male",
        "name": [
          {{
            "family": "Ruecker",
            "given": [
              "Kurt41"
            ]
          }}
        ],
        "identifier": [
          {{
            "system": "http://hl7.org/fhir/sid/us-mbi",
            "value": "{}"
          }}
        ],
        "resourceType": "Patient"
      }}
"""
ROSTER ={'resourceType': 'Group',
 'type': 'person',
 'actual': True,
 'active': True,
 'characteristic': [{'code': {'coding': [{'code': 'attributed-to'}]},
   'valueCodeableConcept': {'coding': [{'system': 'http://hl7.org/fhir/sid/us-npi',
      'code': '2459425221'}]}}],
 'member': []}

FHIR_TYPE = 'application/fhir+json'
FHIR_HEADERS = {'Accept': FHIR_TYPE, 'Content-Type': FHIR_TYPE}

def fhir_headers(org_id=None):
    headers = FHIR_HEADERS.copy()
    if org_id:
        headers['Organization'] = org_id
    return headers

def get(url, headers):
    req = request.Request(url)
    for key, value in headers.items():
        req.add_header(key, value)
    try:
        with request.urlopen(req) as resp:
            return resp
    except URLError as e:
        return False

def delete(url, headers):
    req = request.Request(url, method='DELETE')
    for key, value in headers.items():
        req.add_header(key, value)
    try:
        with request.urlopen(req) as resp:
            return resp
    except URLError as e:
        return False

def post(url, headers, message):
    jsondata = json.dumps(message)
    jsondataasbytes = jsondata.encode('utf-8')
    req = request.Request(url)
    for key, value in headers.items():
        req.add_header(key, value)
    try:
        with request.urlopen(req, jsondataasbytes) as resp:
            return resp, json.loads(resp.read().decode('utf-8'))
    except URLError as e:
        return e, None

def create_organization():
    errors = []
    url = API_BASE + 'Organization/$submit'
    resp, org = post(url, FHIR_HEADERS, ORG_BUNDLE)
    if not resp.status == 200:
        errors.append(f'Expected 200, Received {resp.status}')
    content_type = resp.headers['content-type']
    if not content_type == FHIR_TYPE:
        errors.append(f'Expected Content Type to be {FHIR_TYPE}, Received {content_type}')
    if errors:
        print('Errors in create organization')
        for error in errors:
            print(f'  {error}')
    else:
        print('Successful organization creation')
        return org['id']

def register_providers(org_id):
    errors = []
    url = API_BASE + 'Practitioner/$submit'
    resp, providers = post(url, fhir_headers(org_id), PROVIDERS_BUNDLE)
    if not resp.status == 200:
        errors.append(f'Expected 200, Received {resp.status}')
    content_type = resp.headers['content-type']
    if not content_type == FHIR_TYPE:
        errors.append(f'Expected Content Type to be {FHIR_TYPE}, Received {content_type}')
    if errors:
        print('Errors in register providers')
        for error in errors:
            print(f'  {error}')
    else:
        print('Successful providers registration')
        return [entry['resource']['id'] for entry in providers['entry']]

def register_patients(org_id):
#    return ['16bc78dd-9e4b-427b-b9a6-13efaaa59539', '127fc0ca-56cb-4383-aaeb-ba355d593c68', '6adac1fe-1969-4791-97a0-e4f8b27b5ff0', '35243c42-6e8b-491c-b909-62c02e991f4a', '0ad1b160-09c9-424c-9870-7fd0a01ce096']
    patients_bundle = { "resourceType": "Parameters",
             "parameter": [ { "name": "resource", "resource": { "resourceType": "Bundle",
                                                                "entry": []}}]}
    entries = []
    patients_bundle['parameter'][0]['resource']['entry'] = entries
    for mbi in ('1SQ3F00AA00', '5S58A00AA00', '4S58A00AA00', '3S58A00AA00', '0S80C00AA00',):
        entries.append({ 'resource': json.loads(PATIENT_TEMPLATE.format(mbi)) })

    errors = []
    url = API_BASE + 'Patient/$submit'
    resp, patients = post(url, fhir_headers(org_id), patients_bundle)
    if not resp.status == 200:
        errors.append(f'Expected 200, Received {resp.status}')
    content_type = resp.headers['content-type']
    if not content_type == FHIR_TYPE:
        errors.append(f'Expected Content Type to be {FHIR_TYPE}, Received {content_type}')
    patient_ids = [entry['resource']['id'] for entry in patients['entry']]
    if not len(patient_ids) == 5:
        errors.append(f'Expected 5 patients, received {len(patient_ids)}')
    if errors:
        print('Errors in patients registration')
        for error in errors:
            print(f'  {error}')
    else:
        print('Successful patients registration')
        return patient_ids

def submit_roster(org_id, provider_id, patient_ids):
#    return 'bd2c9f81-e75c-4303-a508-7f1e839dc88f'
    url = API_BASE + 'Group'
    headers = fhir_headers(org_id)
    headers['X-Provenance'] = json.dumps({ "resourceType":"Provenance",
             "meta":{ "profile":[ "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-attestation" ] },
             "recorded": datetime.now(UTC).isoformat(),
             "reason":[ { "system":"http://hl7.org/fhir/v3/ActReason", "code":"TREAT" } ],
             "agent":[ { "role":[ { "coding":[ { "system":"http://hl7.org/fhir/v3/RoleClass", "code":"AGNT" } ] } ],
                         "whoReference":{ "reference":f"Organization/{org_id}" },
                         "onBehalfOfReference":{ "reference":f"Practitioner/{provider_id}" } } ] })
    data = ROSTER
    data['member'] = [{'entity': { 'reference': f'Patient/{patient_id}' } } for patient_id in patient_ids]
    roster = post(url, headers, data)
    return roster['id']

def find_patient_by_mbi(org_id):
    url = API_BASE + 'Patient?identifier=1SQ3F00AA00'

    return get(url, fhir_headers(org_id))

def find_roster_by_npi(org_id):
    url = API_BASE + f'Group?characteristic-value=attributed-to$2459425221'
    return get(url, fhir_headers(org_id))

def remove_patient(org_id, patient_id):
    url = API_BASE + f'Patient/{patient_id}'
def runx():
    provider_ids = register_providers(org_id)
    roster_id = submit_roster(org_id, provider_ids[0], patient_ids)
    find_patient_by_mbi(org_id)
    find_roster_by_npi(org_id)
    print(org_id)
    print(provider_ids)
    print(patient_ids)
    print(roster_id)
    org_id = create_organization()
    provider_ids = register_providers(org_id)
def run():
    org_id = '649aeaf7-a519-4cc4-98bc-2d10f3c0853d'
    patient_ids = register_patients(org_id)
if __name__ == '__main__':
    run()
