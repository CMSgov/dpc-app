The profile defines the required format for Attribution Rosters submitted by Organizations within the DPC application.
It identifies which core elements, extensions, vocabularies and value sets **SHALL** be present in the resource when using this profile.

#### Mandatory Data Elements and Terminology

The following data-elements are mandatory (i.e data MUST be present).
These are presented below in a simple human-readable explanation. 
Profile specific guidance and examples are provided as well. 
The [Formal Profile Definition](#profile) below provides the formal summary, definitions, and terminology requirements.

**Each Group resource must have:**

1. `Group.active` be `true`
1. A group type of `Person`
1. `Group.actual` be `true` 
1. an *attributed-to* relationship in `Group.characteristic`
1. The NPI of the attributed provider in `Group.characteristic.valueCodeableConcept`
1. At least one member `Reference`