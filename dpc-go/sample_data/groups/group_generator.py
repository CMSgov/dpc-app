#! /usr/bin/python3 

import csv
import json
import sys

from copy import deepcopy
from random import randint,  sample


group_template = {
    "resourceType": "Group", 
    "type": "person", 
    "actual": True, 
    "name": "Test Group", 
    "managingEntity": {
        "reference": "Organization/1", 
        "display": "Healthcare related organization"
    }, 
    "member": []
}


member_template = {
    "extension": [
        {
            "url": "http://hl7.org/fhir/us/davinci-atr/StructureDefinition/ext-attributedProvider", 
            "valueReference": {
                "type": "Practitioner", 
                "identifier": {
                    "system": "http://hl7.org/fhir/sid/us-npi", 
                    "value": "NPI_HERE"
                }
            }
        }
    ], 
    "entity": {
        "type": "Patient", 
        "identifier": {
            "system": "http://hl7.org/fhir/sid/us-mbi", 
            "value": "MBI_HERE"
        }
    }
}


mbis = []
with open('mbis.csv', "r") as csvfile:
    mbireader = csv.reader(csvfile)
    for row in mbireader:
        mbis.append(row[0])


def create_group(n):
    if n <= len(mbis):
        pts = "patients" if n > 1 else "patient"
        print("Creating group with",  n,  pts)
        patients = sample(mbis,  n)
        create_members(patients)
    else:
        print("Not enough mbis for",  n,  "patients.")
        exit(1)


def generate_random_npi():
    return str(randint(10**(9),  (10**10)-1))


def output_group(group, npi):
    filename = "group-" + npi + ".json"
    with open(filename,  "w") as write_file:
        json.dump(group,  write_file,  indent=2)
    print("Done: ",  filename, "\n")


def create_members(patients):
    group = deepcopy(group_template)
    count = 0
    npi = generate_random_npi()
    for mbi in patients:
        if count == 100:
            npi = generate_random_npi()
            count = 0
        member = deepcopy(member_template)
        member["extension"][0]["valueReference"]["identifier"]["value"] = npi
        member["entity"]["identifier"]["value"] = mbi
        group["member"].append(member)
        count += 1
    output_group(group, npi)


if len(sys.argv) == 1:
    n = len(mbis)
    create_group(n)
else:
    args = [arg.strip(',') for arg in sys.argv[1:]]
    for arg in args:
        if arg == "0" or arg.startswith("-"):
            print("Groups must have at least 1 member. Skipping", arg, "\n")
            continue
        create_group(int(arg))
