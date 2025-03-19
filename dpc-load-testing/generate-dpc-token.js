import http from 'k6/http';
import encoding from 'k6/encoding';
import { hmac } from 'k6/crypto'

const adminUrl = __ENV.ENVIRONMENT == 'local' ? 'http://host.docker.internal:9903' : __ENV.API_ADMIN_URL;

const fetchGoldenMacaroonURL = `${adminUrl}/tasks/generate-token`

export function fetchGoldenMacaroon() {
  const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/x-www-form-urlencoded'
  };

  return http.post(fetchGoldenMacaroonURL, {}, { headers: headers }).body
}

export function generateDPCToken(orgId, goldenMacaroon) {
  const rawGoldenMacaroon = encoding.b64decode(goldenMacaroon, 'rawurl');
  const macaroon = new Macaroon();
  macaroon.deserialize(rawGoldenMacaroon);
  macaroon.addFirstPartyCaveat('dpc_macaroon_version = 1');
  macaroon.addFirstPartyCaveat(`organization_id = ${orgId}`);
  macaroon.addFirstPartyCaveat(`expires = ${new Date(new Date().getTime() + 5*60000).toISOString()}`);
  const rawToken = macaroon.serialize();
  return encoding.b64encode(rawToken, 'rawurl');
}

const PACKET_PREFIX_LENGTH = 4;
export class Macaroon {
  constructor() {
    this.location = null;
    this.identifier = null;
    this.signature = null;
    this.signatureChunk = null;
    this.caveats = [];
  }

  deserialize(buf) {
    let index = 0;
    while (index < buf.byteLength) {
      const packetLength = new Uint8Array(buf, index, PACKET_PREFIX_LENGTH);
      const len = parseInt(arrayBuffer2String(packetLength), 16);
      const chunk = new Uint8Array(buf, index, len);
      const packet = new Uint8Array(buf, index + PACKET_PREFIX_LENGTH, len - (PACKET_PREFIX_LENGTH + 1));
      const keyValue = arrayBuffer2String(packet).split(" ");
      if (keyValue[0] == 'location') {
	this.location = keyValue[1];
      } else if ( keyValue[0] == 'identifier' ) {
	this.identifier = keyValue[1];
      } else if (keyValue[0] == 'signature') {
	const offset = index + PACKET_PREFIX_LENGTH + 'signature '.length;
	this.signature = new Uint8Array(buf, offset, len - (PACKET_PREFIX_LENGTH + 'signature '.length + 1)) ;
      } else if (keyValue[0] == 'cid') {
	this.caveats.push(keyValue.slice(1).join(' '));
      }
      index = index + len;
    }
  };

  serialize() {
    const arrays = [];
    arrays.push(packetize('location', this.location));
    arrays.push(packetize('identifier', this.identifier));
    for (const caveat of this.caveats) {
      arrays.push(packetize('cid', caveat));
    }
    arrays.push(packetizeSignature(this.signature));
    return typedArrayConcat(arrays);
  };

  addFirstPartyCaveat(caveat) {
    this.caveats.push(caveat);
    const raw = hmac('sha256', this.signature, caveat, 'binary');
    this.signature = new Uint8Array(raw);
  }
};

const packetize = function(key, data) {
  const packetSize = PACKET_PREFIX_LENGTH + 2 + key.length + data.length;
  const packetSizeHex = packetSize.toString(16).padStart(PACKET_PREFIX_LENGTH, '0');
  const packet = `${packetSizeHex}${key} ${data}\n`
  return string2Uint8Array(packet);
}

const packetizeSignature = function(data) {
  const key = 'signature';
  const packetSize = PACKET_PREFIX_LENGTH + 2 + key.length + data.length;
  const packetSizeHex = packetSize.toString(16).padStart(PACKET_PREFIX_LENGTH, '0');
  const header = string2Uint8Array(`${packetSizeHex}${key} `);
  return typedArrayConcat([header, data, string2Uint8Array('\n')]);
}

function typedArrayConcat(arrays) {
  let totalLength = 0;
  for (const arr of arrays) {
    totalLength += arr.length;
  }
  const result = new Uint8Array(totalLength);
  let offset = 0;
  for (const arr of arrays) {
    result.set(arr, offset);
    offset += arr.length;
  }
  return result;
}
export const arrayBuffer2String = function(buffer) {
  const view = new Uint8Array(buffer);
  const arr = Array.from(view);
  return arr.map((x) => String.fromCharCode(x)).join('');
};
export const string2Uint8Array = function(str){
  const bufView = new Uint8Array(str.length);
  for (let i = 0, strLen = str.length; i < strLen; i++) {
    bufView[i] = str.charCodeAt(i);
  }
  return bufView;
};
