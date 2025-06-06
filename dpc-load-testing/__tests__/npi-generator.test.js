import { generateNPI } from "../utils/npi-generator";
import { isArrayUnique } from "../utils/test-utils";
import luhn from "../lib/luhn.js";

describe('generateNPI', () => {
  const npis = []
  beforeAll(() => {
    for (let i = 1; i <= 10000; i++) {
      npis.push(generateNPI(i));
    }
  });

  test('generates a 10 character string', () => {
    npis.every(npi => {
      expect(npi).toHaveLength(10);
    });
  });

  test('generated NPIs pass the luhnCheck', () => {
    npis.every(npi => {
      expect(luhn.isValid("80840" + npi)).toBeTruthy();
    })
  });

  test('generates only unique NPIs', () => {
    expect(isArrayUnique(npis)).toBeTruthy();
  });
})
