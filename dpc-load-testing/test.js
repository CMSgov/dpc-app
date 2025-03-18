import http from 'k6/http';
import encoding from 'k6/encoding';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { sleep } from 'k6';
import { b64encode, b64decode } from 'k6/encoding';
import { fetchGoldenMacaroon } from './generate-dpc-token.js';
import { crypto } from 'k6/experimental/webcrypto';
import { findByNpi } from './dpc-api-client.js';
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
const MACAROON='MDAyM2xvY2F0aW9uIGh0dHA6Ly9sb2NhbGhvc3Q6MzAwMgowMDM0aWRlbnRpZmllciAwNThkNzI3MC1jMWZmLTRmZTItYmQ5Mi05NTVlYzRlMjk1NjgKMDAyZnNpZ25hdHVyZSB0BgPWZYgo1_aybd54qKuapfB8qh-9rP6iBS8MLj-vlgo'
const m1='MDAyM2xvY2F0aW9uIGh0dHA6Ly9sb2NhbGhvc3Q6MzAwMg=='
export default async function () {
  // const goldenMacaroon = fetchGoldenMacaroon();
  // const decodedS = encoding.b64decode(goldenMacaroon, 'rawurl', 's');
  // const decoded = encoding.b64decode(goldenMacaroon, 'rawurl');
  // console.log(decodedS);
  const decoded = encoding.b64decode(MACAROON, 'rawurl');
  const macaroon = new Macaroon();
  macaroon.deserialize(decoded);
  
  const serialized = macaroon.serialize();
  const newMacaroon = encoding.b64encode(serialized, 'rawurl');
  console.log(newMacaroon == MACAROON);
  // const recoded = encoding.b64encode(decoded, 'rawurl');
  // const searchRes = findByNpi('2782823019', '8197402604', newMacaroon);
  // console.log(searchRes.status);
}
