---
title: Data at the Point of Care
layout: default
active: home
---

{% include publish-box.html %}

<!-- { :.no_toc } -->

<!-- TOC  the css styling for this is \pages\assets\css\project.css under 'markdown-toc'-->

* Do not remove this line (it will not be displayed)
{:toc}

<!-- end TOC -->

### Description

Data at the Point of Care (DPC) is a pilot API program that enables healthcare providers to deliver high quality care directly to Medicare beneficiaries by making a patient’s Medicare claims data available to the provider for treatment needs. 
The information can be accessed in the existing workflow and without logging into another application or portal. 
Claims information can be used to confirm information, fill in gaps in care, and improve patient safety.

More information can be found on the Project [website](https://dpc.cms.gov). 


### FHIR Data Model


The primary source of data for the DPC project is the [Blue Button 2.0](https://bluebutton.cms.gov/) project.
We make the underlying FHIR resources available to providers through an efficient bulk access model that complies with the [FHIR Bulk Data Specification](http://hl7.org/fhir/us/bulkdata/2019May/index.html).

Information regarding the Blue Button Data Profiles can be found in the corresponding [Implementation Guide](https://bluebutton.cms.gov/assets/ig/index.html).
