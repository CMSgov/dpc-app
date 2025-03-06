import http from 'k6/http';

const adminUrl = __ENV.ENVIRONMENT == 'local' ? 'http://host.docker.internal:9903' : __ENV.API_ADMIN_URL;
const testOrgId = __ENV.LOAD_TEST_ORGANIZATION_ID;

const fetchGoldenMacaroonURL = `${adminUrl}/tasks/generate-token`
const fetchTokenURL = id => `${fetchGoldenMacaroonURL}?organization=${id}`;

class TokenCache {
  constructor() {
    this.goldenMacaroon = null;
    this.tokens = {};
  }

  setGoldenMacaroon(macaroon) {
    this.goldenMacaroon = macaroon;
  }

  getGoldenMacaroon() {
    return this.goldenMacaroon;
  }

  setToken(orgId, token) {
    this.tokens[orgId] = token;
  }

  getToken(orgId) {
    return this.tokens[orgId];
  }
}

export function fetchGoldenMacaroon() {
  const headers = { 
    'Accept': 'application/json',
    'Content-Type': 'application/x-www-form-urlencoded'
  };

  return http.post(fetchGoldenMacaroonURL, {}, { headers: headers })
}

export function generateDPCToken(orgId) {
  const headers = { 
    'Accept': 'application/json',
    'Content-Type': 'application/x-www-form-urlencoded'
  };

  return http.post(fetchTokenURL(orgId), {}, { headers: headers })
}

const tokenCache = new TokenCache();
export default tokenCache;
