import http from 'k6/http';
import encoding from 'k6/encoding';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { sleep } from 'k6';
import { b64encode, b64decode } from 'k6/encoding';
import { fetchGoldenMacaroon } from './generate-dpc-token.js';
import { crypto } from 'k6/experimental/webcrypto';
import { findByNpi, getOrganization } from './dpc-api-client.js';
import {
  encodeJwt,
  arrayBuffer2String,
  Macaroon,
} from './signing-utils.js';

// See https://grafana.com/docs/k6/latest/using-k6/k6-options/reference for
// details on this configuration object.
export const options = {
  iterations: 1,
};
const MACAROON='MDAyM2xvY2F0aW9uIGh0dHA6Ly9sb2NhbGhvc3Q6MzAwMgowMDM0aWRlbnRpZmllciA1NGQ2MjMwNS01MDcyLTQxNTUtODE4OC0zMTg3ZGM5ODZjZmUKMDAyZnNpZ25hdHVyZSC08NyRP4Ws6g9N2_3qNIZ7h_NWNifv026Di9Hu2gmIdAo'
const CAVEATED='MDAyM2xvY2F0aW9uIGh0dHA6Ly9sb2NhbGhvc3Q6MzAwMgowMDM0aWRlbnRpZmllciA1NGQ2MjMwNS01MDcyLTQxNTUtODE4OC0zMTg3ZGM5ODZjZmUKMDAyMWNpZCBkcGNfbWFjYXJvb25fdmVyc2lvbiA9IDEKMDAyZnNpZ25hdHVyZSD8ZEsh3PE5w11Kzcihm9251hx59qCAhiWQR9keuBsW-go';
export default function () {
  const goldenMacaroon = fetchGoldenMacaroon();
  const decodedMac = encoding.b64decode(goldenMacaroon, 'rawurl');
  const decodedCav = encoding.b64decode(CAVEATED, 'rawurl');
  const macaroon = new Macaroon();
  macaroon.deserialize(decodedMac);
  macaroon.addFirstPartyCaveat('dpc_macaroon_version = 1');
  macaroon.addFirstPartyCaveat("organization_id = c4171882-d99b-4f96-91e7-00f52579001a")
  const serialized = macaroon.serialize();
  // console.log(arrayBuffer2String(decodedCav));
  // console.log(arrayBuffer2String(serialized));
  const newMacaroon = encoding.b64encode(serialized, 'rawurl');
  // console.log(newMacaroon);
  // console.log(newMacaroon == CAVEATED);
  const macCav = new Macaroon();
  macCav.deserialize(decodedCav);
  // console.log(macCav.signature);
  const searchRes = getOrganization(newMacaroon);
  console.log(searchRes.status);
}
