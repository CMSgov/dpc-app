/*global console*/
/* eslint no-console: "off" */
import { check, sleep } from 'k6';
import {
  authorizedGet,
} from '../dpc-api-client.js';

// We allow errors in local because we don't need to test our own connection to BFD
const JOB_OUPUT_ERROR_LENGTH = __ENV.ENVIRONMENT == 'local' ? 1 : 0;

// Our WAF rate limits us to 300 requests every 5 minutes, so don't poll too often
const EXPORT_POLL_INTERVAL_SEC = __ENV.ENVIRONMENT == 'local' ? 1 : 20;

export async function monitorJob(token, jobUrl){
  // Loop until it isn't 202
  // NB: because we are not refreshing the token, it will eventually get a 401
  let jobResponse = authorizedGet(token, jobUrl);
  while(jobResponse.status === 202){
    sleep(EXPORT_POLL_INTERVAL_SEC);
    jobResponse = authorizedGet(token, jobUrl);
  }

  // We got a rare exception when testing, so try is to make sure we capture the problem
  try {
    const checkJobResponse = check(
      jobResponse,
      {
        'job response code was 200': res => res.status === 200,
        'no job output errors': res => res.json().error.length <= JOB_OUPUT_ERROR_LENGTH,
      }
    );

    if (!checkJobResponse) {
      if (jobResponse.status == 401) {
        console.error(`JOB TIMED OUT FOR TEST - MAYBE NOT FAIL: ${jobUrl}`);
      } else if (jobResponse.json().error) {
        console.error(`Too many errors in job output ${jobResponse.json().error.length}: ${jobUrl}`);
      } else {
        console.error(`Bad response code when checking job output ${jobResponse.status} ${jobUrl}`);
      }
    }
  } catch (error) {
    console.error(`Error thrown parsing ${jobResponse.body}: ${jobUrl}`)
    console.error(error);
    exec.test.fail();
  }
}
