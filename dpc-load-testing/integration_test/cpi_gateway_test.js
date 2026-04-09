/*global console*/
/* eslint no-console: "off" */

import { check } from 'k6';
import exec from 'k6/execution';
import { isEmptyObject, isObjectType, isDate, isArrayType } from '../utils/test-utils.js';
import { getToken } from '../oauth-client.js';
import http from 'k6/http';

var PROVIDERS_PATH = "api/1.0/ppr/providers";
var ORG_PROVIDER_PROFILE_PATH = "api/1.0/ppr/providers/profile";


//var PROFILE_PATH = "api/1.0/ppr/providers/profiles";

// No longer needed as we started to use SSM Parameter Store to aid local dev and testing.
//var testData = __ENV.ENVIRONMENT == 'local' ? JSON.parse(open('./cpi_test_data.secret')) : {}

export const options = {
    thresholds: {
        checks: ['rate===1'],
    },
    insecureSkipTLSVerify: true,
    scenarios: {
        smokeTests: {
            executor: 'per-vu-iterations',
            vus: 1,
            iterations: 1,
            exec: "runCPITests"

        }
    }
};

// This function aggregates the config data from different sources based upon 
// the environment and presents unified source for subsequent tests.
function getConfig() {
    return {
        ...JSON.parse(__ENV.CPI_API_GW_TESTDATA),
        ...{ clientId: __ENV.CLIENT_ID, clientSecret: __ENV.CLIENT_SECRET }
    };
}

export function setup() {

    var env = __ENV.ENVIRONMENT;
    console.log(`Running CPI Gateway Integration Test with environment ${env}`);

    var config = getConfig();
    //console.log(`Merged Config for CPI Gateway Integration Test: ${JSON.stringify(config)}`);

    if (isEmptyObject(config)) {
        console.error("\nTest data not found in environment variable CPI_API_GW_TESTDATA");
        exec.test.abort('failed to check for existing orgs');
    }

    var cpiConfig = config.meta.configuration['test'];

    const oauthEndpointBaseUrl = cpiConfig["OAUTH_URL"];
    const oauthEndpointTokenPath = cpiConfig["TOKEN_ENDPOINT"];
    const clientId = config.clientId;
    const clientSecret = config.clientSecret;

    if (!oauthEndpointBaseUrl || !oauthEndpointTokenPath || !clientId || !clientSecret) {
        console.error("Missing required environment variables for OAuth token retrieval");
        exec.test.abort("Error: missing environment variables");
    }

    const baseUrl = `${oauthEndpointBaseUrl}/${oauthEndpointTokenPath}`;
    const token = getToken(baseUrl, clientId, clientSecret, "READ");
    if (!token) {
        console.error("Failed to retrieve access token for CPI Gateway Integration Test");
        exec.test.abort("Error: Failed to retrieve access token");
    }

    return {
        cpiBaseUrl: cpiConfig["BASE_URL"],
        token: token,
        testData: config.data
    };

}

export function runCPITests(params) {
    //console.log("Running CPI Gateway Integration Test with config: " + JSON.stringify(params));
    const { cpiBaseUrl, token, testData } = params;

    var testDataForMedSanctions = testData["AO_WITH_MED_SANCTIONS"];
    const providerResponse = getProvider(cpiBaseUrl, token, "ind", "ssn", testDataForMedSanctions.ao_ssn);

    check(
        providerResponse,
        {
            'get provider returns 200': res => res.status == 200,
            'provider data type': res => isObjectType(res.json(), 'provider'),
            'provider data returned': res => !isEmptyObject(res.json().provider),
            'med sanctions data returned': res => isArrayType(res.json().provider, 'medSanctions') &&
                res.json().provider.medSanctions.length > 0,
            'med sanction code returned': res => res.json().provider.medSanctions[0].sanctionCode != null,
            'med sanction date returned in right format': res => isDate(res.json().provider.medSanctions[0].sanctionDate),
        }
    )

    var testDataForWaivers = testData["AO_WITH_WAIVERS"];
    const waiverResponse = getProvider(cpiBaseUrl, token, "ind", "ssn", testDataForWaivers.ao_ssn);
    check(
        waiverResponse,
        {
            'get provider returns 200': res => res.status == 200,
            'provider data type is object': res => isObjectType(res.json(), 'provider'),
            'provider data returned': res => !isEmptyObject(res.json().provider),
            'waiver data returned': res => isArrayType(res.json().provider, 'waiverInfo') &&
                res.json().provider.waiverInfo.length > 0,
            'waiver effectiveDate returned': res => res.json().provider.waiverInfo[0].effectiveDate != null &&
                isDate(res.json().provider.waiverInfo[0].effectiveDate),
            'waiver endDate valid': res => res.json().provider.waiverInfo[0].endDate &&
                isDate(res.json().provider.waiverInfo[0].endDate),
        }
    )

    var testDataForUnapprovedEnrollmentStatus = testData["UNAPPROVED_ENROLLMENT_STATUS"];
    const orgProviderResponse = getProviderOrg(cpiBaseUrl, token, testDataForUnapprovedEnrollmentStatus.org_npi);
    check(
        orgProviderResponse,
        {
            'get org provider returns 200': res => res.status == 200,
            'org provider data type is object': res => isObjectType(res.json(), 'provider'),
            'org provider data returned': res => !isEmptyObject(res.json().provider),
            'enrollments data returned': res => isArrayType(res.json().provider, 'enrollments') &&
                res.json().provider.enrollments.length > 0,
            'unapproved enrollment status': res => res.json().provider.enrollments.every(
                enrollment => enrollment.status != "APPROVED")
        }
    )

    var testDataForOrgWithAOInfo = testData["ORG_WITH_AO_SSN"];
    const orgProviderWithAOResponse = getProviderOrg(cpiBaseUrl, token, testDataForOrgWithAOInfo.org_npi);
    const ssnNoHyphens = /^\d{9}$/;
    check(
        orgProviderWithAOResponse,
        {
            'get org provider returns 200': res => res.status == 200,
            'org provider data type is object': res => isObjectType(res.json(), 'provider'),
            'org provider data returned': res => !isEmptyObject(res.json().provider),
            'enrollments data returned': res => isArrayType(res.json().provider, 'enrollments') &&
                res.json().provider.enrollments.length > 0,
            'approved enrollment status': res => res.json().provider.enrollments.some(
                enrollment => enrollment.status === "APPROVED"),
            'roles data returned': res => {
                let activeEnrollment = res.json().provider.enrollments.find(enrollment => enrollment.status === "APPROVED");
                return !!activeEnrollment && isArrayType(activeEnrollment, 'roles') && activeEnrollment.roles.length > 0;
            },
            'AO info returned for org': res => {
                let activeEnrollment = res.json().provider.enrollments.find(enrollment => enrollment.status === "APPROVED");
                let roles = activeEnrollment ? activeEnrollment.roles : [];
                return roles.some(role => role.roleCode === "10"
                    && role.dataIndicator === "CURRENT"
                    && !!role.ssn
                    && ssnNoHyphens.test(role.ssn)
                );
            }

        }
    )

}

function getProvider(cpiBaseUrl, token, type, idType, id) {
    const url = `${cpiBaseUrl}/${PROVIDERS_PATH}`;
    const payload = providerRequest(type, idType, id);
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
    };
    return http.post(url, payload, params);

}

function getProviderOrg(cpiBaseUrl, token, orgNpi) {
    const url = `${cpiBaseUrl}/${ORG_PROVIDER_PROFILE_PATH}`;
    const payload = orgProviderRequest(orgNpi);
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
    };
    return http.post(url, payload, params);
}

function orgProviderRequest(npi) {
    return JSON.stringify({ "providerID": { "npi": npi } });
}

function providerRequest(type, idType, id) {
    return JSON.stringify(
        {
            "providerID": {
                "providerType": type,
                "identity": {
                    "idType": idType,
                    "id": id
                }
            },
            "dataSets": {
                "all": true
            }
        });
}








