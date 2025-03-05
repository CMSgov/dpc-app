import http from 'k6/http';

const adminUrl = __ENV.ENVIRONMENT == 'local' ? 'http://host.docker.internal:9903' : __ENV.API_ADMIN_URL;
const testOrgId = __ENV.LOAD_TEST_ORGANIZATION_ID;

const fetchTokenURL = id => `${adminUrl}/tasks/generate-token?organization=${id}`;

export default function generateDPCToken(orgId) {
  const headers = { 
    'Accept': 'application/json',
    'Content-Type': 'application/x-www-form-urlencoded'
  };

  return http.post(fetchTokenURL(orgId), {}, { headers: headers })
}
