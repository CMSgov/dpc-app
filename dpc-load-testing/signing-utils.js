import { CryptoKey, crypto } from 'k6/experimental/webcrypto';
import encoding from 'k6/encoding';

export const string2ArrayBuffer = function(str) {
  const buf = new ArrayBuffer(str.length);
  const bufView = new Uint8Array(buf);
  for (let i = 0, strLen = str.length; i < strLen; i++) {
    bufView[i] = str.charCodeAt(i);
  }
  return buf;
};
export const string2Uint8Array = function(str){
  const bufView = new Uint8Array(str.length);
  for (let i = 0, strLen = str.length; i < strLen; i++) {
    bufView[i] = str.charCodeAt(i);
  }
  return bufView;
};

export const arrayBuffer2String = function(buffer) {
  const view = new Uint8Array(buffer);
  const arr = Array.from(view);
  return arr.map((x) => String.fromCharCode(x)).join('');
};

export const encodeJwt = async function(header, payload, key) {
  const headerString = encoding.b64encode(JSON.stringify(header), 'rawurl');
  const payloadString = encoding.b64encode(JSON.stringify(payload), 'rawurl');

  const signatureBuffer = await crypto.subtle.sign(
    {
      name: "RSASSA-PKCS1-v1_5",
    },
    key,
    string2ArrayBuffer([headerString, payloadString].join('.'))
  );

  const signature = encoding.b64encode(signatureBuffer, 'rawurl');
  return [headerString, payloadString, signature].join('.');
};

const PACKET_PREFIX_LENGTH = 4;
export class Macaroon {
  constructor() {
    this.location = null;
    this.identifier = null;
    this.signature = null;
    this.signatureChunk = null;
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
      }
      index = index + len;
    }
  };
  serialize() {
    const locationArray = packetize('location', this.location);
    const identifierArray = packetize('identifier', this.identifier)
    const signatureArray = packetizeSignature(this.signature);
    return typedArrayConcat(locationArray, identifierArray, signatureArray);
  }
};

const packetize = function(key, data) {
  const packetSize = PACKET_PREFIX_LENGTH + 2 + key.length + data.length;
  const packetSizeHex = packetSize.toString(16).padStart(PACKET_PREFIX_LENGTH, '0');
  const packet = `${packetSizeHex}${key} ${data}\n`
  return string2Uint8Array(packet); // string2ArrayBuffer(`${packetSizeHex}${key} ${data}`);
}

const packetizeSignature = function(data) {
  const key = 'signature';
  const packetSize = PACKET_PREFIX_LENGTH + 2 + key.length + data.length;
  const packetSizeHex = packetSize.toString(16).padStart(PACKET_PREFIX_LENGTH, '0');
  const header = string2Uint8Array(`${packetSizeHex}${key} `);
  return typedArrayConcat(header, data, string2Uint8Array('\n'));
}

function typedArrayConcat(...arrays) {
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
