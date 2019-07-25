The profile defines the required format for a Patient resource submitted by an Organization as a part of their patient rostering workflow.
It identifies which core elements, extensions, vocabularies and value sets **SHALL** be present in the resource when using this profile.

> Note: This profile differs from the Patient resource returned by the export Operation, which uses the BlueButton 2.0 [Patient Profile](https://bluebutton.cms.gov/assets/ig/StructureDefinition-bluebutton-patient-claim.html).

#### Mandatory Data Elements and Terminology

The following data-elements are mandatory (i.e data MUST be present).
These are presented below in a simple human-readable explanation. 
Profile specific guidance and examples are provided as well. 
The [Formal Profile Definition](#profile) below provides the formal summary, definitions, and terminology requirements.

**Each Patient must have:**

1. an id
1. a Medicare Beneficiary ID (MBI)
1. a given name
1. a family name
1. a birth date
1. a Managing Organization
