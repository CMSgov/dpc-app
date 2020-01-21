The profile defines the required format for Attribution Attestations submitted by Organizations within the DPC application.
It identifies which core elements, extensions, vocabularies and value sets **SHALL** be present in the resource when using this profile.

#### Mandatory Data Elements and Terminology

The following data-elements are mandatory (i.e data MUST be present).
These are presented below in a simple human-readable explanation. 
Profile specific guidance and examples are provided as well. 
The [Formal Profile Definition](#profile) below provides the formal summary, definitions, and terminology requirements.

**Each Group resource must have:**

1. A `Provenance.reason` to have the code `http://hl7.org/fhir/v3/ActReason#TREAT`
1. A `Provenance.recorded` with the current date
1. At least 1 `Provenance.agent` with the following properties:
    - `Provenance.agent.role` with the code `http://hl7.org/fhir/v3/RoleClass#AGNT`
    - `Provenance.agent.who` with a reference to the `Organization` making the attribution request
    - `Provenance.agent.onBehalfOf` with a reference to the `Practitioner` on which the attribution request is being made for