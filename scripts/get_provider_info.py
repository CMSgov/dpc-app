import csv
import json
import requests

with open('all_npis.csv') as file:
    all_npis = [npi[0] for npi in csv.reader(file, delimiter='\n')][1:]

auth_token = "<token>"
request_url = "https://val.cpiapi.cms.gov/api/1.0/ppr/providers"
headers = {
    'Authorization': 'Bearer ' + auth_token,
    'Content-Type': 'application/json'
}

total_sanctioned = 0
total_waived = 0
total_fala = 0

for npi in all_npis:
    post_body = {
        "providerID": {
            "npi": npi
        },
        "dataSets": {
            "all": True
        }
    }
    response = requests.post(request_url, headers=headers, data=json.dumps(post_body), verify=False)
    if response.status_code != 200:
        continue
    provider = response.json().get("provider")
    if not provider:
        continue
    medSanctions = provider.get("medSanctions")
    waiverInfo = provider.get("waiverInfo")
    fala = provider.get("fala")
    if not (medSanctions or waiverInfo or fala):
        continue
    total_sanctioned += int(medSanctions is not None)
    total_waived += int(waiverInfo is not None)
    total_fala += int(fala is not None)
