import { generateNPI } from "../utils/npi-generator";
import { isArrayUnique } from "../utils/test-utils";

function luhnCheck(npi) {
  const appendedNpi = "80840" + npi;
  const reversed = appendedNpi.split('').reverse();
  let total = 0;
  for (const [index, value] of reversed.entries()) {
    let x = parseInt(value);
    if (index % 2 === 1) {
      x = x * 2;
      // if the result has two digits, add the digits together to get a single digit number
      if (x > 9) {
        x.toString().split('').map(Number).reduce((a, b) => a + b);
      }
    }

    total += x;
  }

  return total % 10 === 0;
}

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
      expect(luhnCheck(npi)).toBeTruthy();
    })
  });

  test('generates only unique NPIs', () => {
    expect(isArrayUnique(npis)).toBeTruthy();
  });
})
