import { check, fail } from 'k6';
import encoding from 'k6/encoding';
import { Macaroon, packetize, packetizeSignature, arrayBuffer2String } from './generate-dpc-token.js';


export const options = {
  scenarios: {
    testSerialization: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: "testSerialization" },
    testDeserialization: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: "testDeserialization" },
    testAddCaveat: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: "testAddFirstPartyCaveat" },
    testPacketizers: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: "testPacketizers" },
  }
};

const MACAROON='MDAwZmxvY2F0aW9uIGMKMDAxMWlkZW50aWZpZXIgYgowMDJmc2lnbmF0dXJlIMW8i72_XoEghAnjnQd6lt4eKFxKDQupCSNDbWhTkJVGCg'

export function testSerialization(data) {
  const macaroon = builtMacaroon();
  const serialized = serialzedB64Macaroon(macaroon);
  const checkRoundTrip = check(
    serialized,
    { 'matches initial MACAROON': serialized => serialized === MACAROON }
  )
  if (!checkRoundTrip) {
    console.log('Round trip did not produce same MACAROON');
    return;
  }
  const expectedWithCaveat = 'MDAwZmxvY2F0aW9uIGMKMDAxMWlkZW50aWZpZXIgYgowMDBlY2lkIGQgPSBlCjAwMmZzaWduYXR1cmUg79yFqm7BFHnKMvm1CWpPuFc_CC42M7pwoomYKc_AAycK';
  macaroon.addFirstPartyCaveat('d = e');
  const serializedWithFPC = serialzedB64Macaroon(macaroon);
  const checkFPC = check(
    serializedWithFPC,
    { 'matches macaroon with caveat': serialized => serialized === expectedWithCaveat }
  )
  if (!checkFPC) {
    console.log('Macaroon with caveat did not produce correct macaroon');
    return;
  }
};
 
export function testDeserialization() {
  const macaroon = builtMacaroon();
  const checkLocation = check(
    macaroon,
    { 'matches location': macaroon => macaroon.location === 'c' }
  )
  if (!checkLocation) {
    console.log('Macaroon location should be c, was', macaroon.location);
  }
  const checkIdentifier = check(
    macaroon,
    { 'matches identifier': macaroon => macaroon.identifier === 'b' }
  )
  if (!checkIdentifier) {
    console.log('Macaroon identifier should be c, was', macaroon.identifier);
  }
  const checkSignature = check(
    macaroon,
    { 'matches signature': macaroon => macaroon.signature !== null }
  )
  if (!checkSignature) {
    console.log('Macaroon signature should not be null');
  }
}

export function testAddFirstPartyCaveat() {
  const macaroon = builtMacaroon();
  const macaroonSignature = macaroon.signature;
  macaroon.addFirstPartyCaveat('d = e');
  
  const checkCaveatLength = check(
    macaroon,
    { 'matches caveat length': macaroon => macaroon.caveats.length === 1 }
  )
  if (!checkCaveatLength) {
    console.log('Macaroon caveat length should be 1, was', macaroon.caveats.length);
    return
  }
  
  const checkCaveatValue = check(
    macaroon,
    { 'matches caveat value': macaroon => macaroon.caveats[0] === 'd = e' }
  )
  if (!checkCaveatValue) {
    console.log('Macaroon caveat value should be d = e, was', macaroon.caveats[0]);
    return
  }

  const checkSignatureChange = check(
    macaroon,
    { 'new signature does not match old': macaroon => macaroon.signature !== macaroonSignature }
  )
  if (!checkSignatureChange) {
    console.log('Signature after adding caveat should change, but did not');
  }
}

export function testPacketizers() {
  const expectedPacket = "000ekey value\n"
  const packet = packetize('key', 'value');
  const checkPacket = check(
    packet,
    { 'matches expected packet': packet => arrayBuffer2String(packet) === expectedPacket }
  );
  if (!checkPacket) {
    console.log(`expected ${arrayBuffer2String(packet)} to equal ${expectedPacket}`);
  }

  const expectedSigPacket = "0014signature \x00\x00\x00\x00\x00\n"
  const sigPacket = packetizeSignature('value');
  const checkSigPacket = check(
    sigPacket,
    { 'matches expected packet': packet => arrayBuffer2String(packet) === expectedSigPacket }
  );
  if (!checkSigPacket) {
    console.log(`expected ${arrayBuffer2String(sigPacket)} to equal ${expectedSigPacket}`);
  }
};

export function handleSummary(data) {
  const fails = data.root_group.checks.map(x => x.fails).reduce((mem, x) => { return mem + x }, 0);
  console.log('Fails:', fails);
  if (__ENV.ENVIRONMENT == 'local') {
    return { stdout: `Number of failed tests: ${fails.toString()}` };
  } else {
    return { '/test-results/macaroons-fail-count.txt': fails.toString() };
  }
}

function builtMacaroon() {
  const macaroonData = encoding.b64decode(MACAROON, 'rawurl');
  const macaroon = new Macaroon();
  macaroon.deserialize(macaroonData);
  return macaroon;
}

function serialzedB64Macaroon(macaroon) {
  const serialized = macaroon.serialize();
  return encoding.b64encode(serialized, 'rawurl');
}
