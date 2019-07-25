The profile defines the required format for Address datatypes for Organization resources within the DPC application.
It identifies which core elements, extensions, vocabularies and value sets **SHALL** be present in the resource when using this profile.

#### Mandatory Data Elements and Terminology

The following data-elements are mandatory (i.e data MUST be present).
These are presented below in a simple human-readable explanation. 
Profile specific guidance and examples are provided as well. 
The [Formal Profile Definition](#profile) below provides the formal summary, definitions, and terminology requirements.

**Each Address element must have:**

1. an address use
1. an address type
1. an address line
1. a city
1. a state
1. a postal code
1. a country

**If the data is available an Address shall include:**

1. a district