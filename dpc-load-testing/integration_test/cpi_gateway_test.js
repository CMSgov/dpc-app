/*global console*/
/* eslint no-console: "off" */

import { check } from 'k6';
import exec from 'k6/execution';
import { isEmptyObject, isObjectType, isDate, getToken, isArrayType } from '../utils/test-utils.js';
import http from 'k6/http';

var PROVIDERS_PATH = "api/1.0/ppr/providers";
var PROFILE_PATH = "api/1.0/ppr/providers/profiles";
var testData = __ENV.ENVIRONMENT == 'local' ? JSON.parse(open('./cpi_test_data.secret')) : {}

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
    if (__ENV.ENVIRONMENT != 'local') {
        return {
            ...JSON.parse(__ENV.CPI_API_GW_TESTDATA),
            ...{ clientId: __ENV.CLIENT_ID, clientSecret: __ENV.CLIENT_SECRET }
        };
    } else {
        return {
            ...testData, ...{
                clientId: __ENV.CLIENT_ID,
                clientSecret: __ENV.CLIENT_SECRET
            }
        };
    }
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
    const providerResponse = getDataForProvider(cpiBaseUrl, token, "ind", "ssn", testDataForMedSanctions.ao_ssn);

    const checkProviderResponse = check(
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
    const waiverResponse = getDataForProvider(cpiBaseUrl, token, "ind", "ssn", testDataForWaivers.ao_ssn);
    const checkWaiverResponse = check(
        waiverResponse,
        {
            'get provider returns 200': res => res.status == 200,
            'provider data type': res => isObjectType(res.json(), 'provider'),
            'provider data returned': res => !isEmptyObject(res.json().provider),
            'waiver data returned': res => isArrayType(res.json().provider, 'waiverInfo') &&
                res.json().provider.waiverInfo.length > 0,
            'waiver effectiveDate returned': res => res.json().provider.waiverInfo[0].effectiveDate != null &&
                isDate(res.json().provider.waiverInfo[0].effectiveDate),
            'waiver endDate valid': res => res.json().provider.waiverInfo[0].endDate &&
                isDate(res.json().provider.waiverInfo[0].endDate),
        }
    )
}

function getDataForProvider(cpiBaseUrl, token, type, idType, id) {
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

function getProviderOrganization(cpiBaseUrl, token, npi) {
    const url = `${cpiBaseUrl}/${PROFILE_PATH}`;
    const payload = JSON.stringify(
        {
            "providerID": {
                "npi": npi
            }
        });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
    };
    return http.post(url, payload, params);

}








