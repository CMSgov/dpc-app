
## Overview
If you’ve found yourself on this page, it’s because you’re interested in building software that helps the more than 53 million Medicare beneficiaries in the United States. Thank you.

Please read our Application Programming Interface (API) Terms of Service carefully and post any questions you may have to the [Google Group](https://groups.google.com/d/forum/dpc-api).

### Scope
By accessing or using Centers for Medicare & Medicaid Services (CMS) Data at the Point of Care APIs and other services (collectively, Data APIs), you are agreeing to the terms below, as well as any relevant sections of [CMS’s Privacy Policies](https://www.cms.gov/About-CMS/Agency-Information/Aboutwebsite/Privacy-Policy.html) (collectively, Terms).

## Data Rights and Usage

### Accounts/Registration
If you are using the Data APIs on behalf of an entity, you represent and warrant that you have authority to bind that entity to the Terms and by accepting the Terms, you are doing so on behalf of that entity (and all references to “you” in the Terms refer to you and that entity).

In order to access the Data APIs you may be required to provide certain information (such as identification or contact details) as part of the registration process for the Data APIs, or as part of your continued use of the Data APIs. Any registration information you give to CMS must be accurate and up to date and you must inform CMS promptly of any updates so that we can keep you informed of any changes to the Data APIs or the Terms which may impact your usage of the Data APIs.

Credentials (such as passwords, keys, tokens, and client IDs) issued to you by CMS for Data at the Point of Care are intended to be used only by you and to identify any software which you are using with the Data APIs. You agree to keep your credentials confidential and make reasonable efforts to prevent and discourage other persons or entities from accessing or using your credentials.  Credentials may not be embedded in open source projects.

You may only access (or attempt to access) the Data APIs by the means described in the documentation of the Data APIs. If CMS assigns you credentials, you must use them with the applicable Data APIs.  CMS may revoke your credentials if you use or attempt to use them with another application that has not been reviewed and approved by CMS.

If you are granted production application credentials for the Data APIs, you may only use those credentials with the application that passed the production access review process. CMS may revoke your production application credentials if you use or attempt to use them with another application or product that has not been reviewed and approved by CMS.

### Activities and Purposes

You may use the Data APIs to develop a service to notify, search, display, analyze, retrieve, view, and otherwise obtain certain information or data about Medicare beneficiaries or synthetic data from CMS, specifically: Part A, Part B, and Part D beneficiary claims information, such as providers seen, diagnoses, medications, and procedures.

### Privacy

Information or data about Medicare beneficiaries from CMS available from the Data APIs is subject to the Privacy Act of 1974, the Health Insurance Portability and Accountability Act  of 1996(HIPAA), and other laws, and require special safeguarding. You must comply with all applicable federal and state laws regarding the protection and disclosure of information obtained through the Data APIs.

Data can only be requested through the Data API by a HIPAA covered entity, or a business associate on behalf of a covered entity, as those terms are defined in 45 CFR § 160.103, provided that the covered entity or business associate attests that the request is for purposes of treatment as defined in 45 CFR § 164.501 for each individual for which data is requested. Once a patient roster record expires, at 90 days for a standard roster or 2 days for emergency room visit rosters, based on the reason for the request, you will be responsible for attesting on each roster addition and renewal that the request is for the purpose of the individual added to the roster and that you still have a valid treatment reason as defined by HIPAA for the specific individual before you will receive any updated information for the individual.

You further acknowledge that when records regarding an individual are obtained through a Data API, you may not disclose any information or data regarding the individual to any other individuals or third parties without specific, explicit consent from the individual or his or her authorized representative. The terms “individual” and “record” have the meanings given in the Privacy Act at 5 U.S.C. § 552a(a). [Click here if you would like more information about the application of the Privacy Act at CMS](https://www.cms.gov/Research-Statistics-Data-and-Systems/Computer-Data-and-Systems/Privacy/PrivacyActof1974.html).


### Attribution

When using content, data, documentation, code, and related materials associated with the Data APIs in your own work, proper credit must be given.  All services that utilize or access the Data APIs must display the following notice prominently within the application:

“This product uses the Data APIs but is not endorsed or certified by the Centers for Medicare & Medicaid Services or the U.S. Department of Health and Human Services.”

You may use CMS’s name or logo in order to identify the source of API content subject to these Terms. You may not use the CMS name, logo, or the like to imply endorsement of any product, service, or entity, not-for-profit, commercial or otherwise.

- [Blue Button branding guidelines](https://bluebutton.cms.gov/developers/#branding-guidelines)
- [HealthIT.gov logo and usage guidelines](https://www.healthit.gov/topic/health-it-initiatives/blue-button/logo-and-usage)

### Service Management

#### Right to Limit

Your use of the Data APIs will be subject to certain limitations on access, calls, or use as set forth within these Terms or otherwise provided by CMS. These limitations are designed to manage the load on the system, promote equitable access, and prevent abuse, and these limitations may be adjusted without notice, as deemed necessary by CMS. If CMS reasonably believes that you have attempted to exceed or circumvent these limits, your ability to use the Data APIs may be temporarily or permanently blocked. CMS may monitor your use of the Data APIs to, for example, improve the service or to ensure compliance with these Terms.

#### Service Terminations

If you wish to terminate your agreement with these Terms, you may do so by refraining from further use of the Data APIs. CMS reserves the right (though not the obligation) to: (1) refuse to provide the Data APIs to you, if CMS determines that use violates any CMS policy, including these Terms; or (2) terminate or deny you access to and use of all or part of the Data APIs at any time for any other reason which in its sole discretion it deems necessary in order to prevent abuse. You may petition CMS to regain access to the Data APIs through the support email address provided by CMS for the Data APIs. If CMS determines in its sole discretion that the circumstances which led to the refusal to provide the Data APIs or terminate access to the Data APIs no longer exist, then CMS may restore your access. All provisions of these Terms, shall survive termination, including, without limitation, warranty disclaimers, and limitations of liability.

### Security
In order to use the Data APIs, upon registration and each time any software submits, requests, or retrieves information from the Data APIs, you must attest that the software meets the security requirements for the Data APIs, specifically one or more of the following requirements:

- Completed and holds an active ONC Health IT Certification
- Active HITRUST self-validation assessment (valid for one year from implementation if currently pursuing the HITRUST validated assessment)
- Active HITRUST Validated Assessment

### Other Responsibilities

- You must be fully accountable for all transactions you submit and will cooperate with CMS or its agents in the event that CMS has a security concern with respect to any inquiry, submission, or receipt of information to or from CMS.
- You must promptly inform CMS in the event you identify misuse of “individually identifiable” health information you accessed from the CMS database.
- You must promptly inform CMS or one of CMS’ contractors in the event that you no longer meet any of this terms of service.
- You must immediately cease use of the Data APIs when you no longer meet any of the terms of this terms of service.
- You must adhere to the basic desktop security measures to ensure the security of Medicare beneficiary personal health information.

### Vendor Responsibilities

Vendor agrees to use sufficient security procedures (including compliance with all provisions of the HIPAA security regulations) to ensure that all data transmissions are authorized and protect all beneficiary-specific data from improper access. You are responsible for the privacy and security of API transactions on behalf of providers.

As a vendor submitting API transactions and receiving data on behalf of a HIPAA-covered provider:

- Vendor must not use the APIs except as an authorized agent of the health care provider and pursuant to a business associate contract, as required by 45 C.F.R. §§ 164.314(a) and 164.504(e), with the health care provider.
- The clearinghouse must be able to associate each inquiry with the Medicare FFS provider for each API call. That is, for each inquiry a vendor makes, that vendor must be able to identify the Medicare FFS provider making the request for each beneficiary’s information and be able to assure that responses are routed only to the submitter that originated each request.
- The vendor will release data only to active Medicare FFS providers or their authorized agents as needed for treatment purposes, including care coordination. The clearinghouse will not disclose information to anyone other than the Medicare FFS provider.
- CMS will prohibit or suspend clearinghouse access if there is a record of violation that would indicate that beneficiary data could be at risk of improper disclosure due to access the Clearinghouse approved.


### Liability

#### Disclaimer of Warranties

The Data API platform is provided “as is” and on an “as-available” basis. While we will do our best to ensure the service is available and functional at all times, CMS hereby disclaims all warranties of any kind, express or implied, including without limitation the warranties of merchantability, fitness for a particular purpose, and non-infringement. CMS makes no warranty that data will be error free or that access thereto will be continuous or uninterrupted.

### Limitations on Liability

In no event will CMS or the U.S. Department of Health and Human Services (HHS) be liable with respect to any subject matter of this Agreement under any contract, negligence, strict liability or other legal or equitable theory for: (1) any special, incidental, or consequential damages; (2) the cost of procurement of substitute products or services; or (3) for interruption of use or loss or corruption of data.

#### Disputes, Choice of Law, Venue, and Conflicts

Any disputes arising out of this Agreement and access to or use of the API shall be governed by the laws and common law of the United States of America, including without limitation such regulations as may be promulgated from time to time by CMS, HHS, or any of its constituent agencies, without regard to any conflict of laws statutes or rules. You further agree and consent to the jurisdiction of the Federal Courts located within the District of Columbia and the courts of appeal therefrom, and waive any claim of lack of jurisdiction or forum non conveniens. Some APIs may have API-specific terms of use. If there is a conflict between these Terms and additional terms applicable to a specific API, the terms applicable to the specific API will control.

#### Indemnification

You agree to indemnify and hold harmless HHS, including CMS, its contractors, employees, agents, and the like, from and against any and all claims and expenses, including attorney’s fees, arising out of your use of the API, including but not limited to violation of this Agreement.

#### No Waiver of Rights

CMS’s failure to exercise or enforce any right or provision of this Agreement shall not constitute waiver of such right or provision.