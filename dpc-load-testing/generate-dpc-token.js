import http from 'k6/http';

const adminUrl = __ENV.ENVIRONMENT == 'local' ? 'http://host.docker.internal:9903' : __ENV.API_ADMIN_URL;
const testOrgId = __ENV.LOAD_TEST_ORGANIZATION_ID;

const fetchTokenURL = `${adminUrl}/tasks/generate-token?organization=${testOrgId}`;


export default function generateDPCToken() {
  const headers = { 
    'Accept': 'application/json',
    'Content-Type': 'application/x-www-form-urlencoded'
  };

  return http.post(fetchTokenURL, {}, { headers: headers })
}
