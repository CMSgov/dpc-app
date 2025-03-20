/**
 * Returns a Luhn check digit, which should be the last digit of an NPI. This serves as a 
 * validation mechanism. 
 * @param {string} nineDigitNumber 
 * @returns {string} a digit to be appended to the nineDigitNumber
 */
function generateLuhnCheckDigit(nineDigitNumber) {
  const appendedNineDigitNumber = "80840" + nineDigitNumber;
  const reversed = appendedNineDigitNumber.split('').reverse();
  let total = 0;
  for (const [index, value] of reversed.entries()) {
    let x = parseInt(value);
    if (index % 2 === 0) {
      x = x * 2;
      // if the result has two digits, add the digits together to get a single digit number
      if (x > 9) {
        x.toString().split('').map(Number).reduce((a, b) => a + b);
      }
    }

    total += x;
  }

  const unitsDigit = total % 10;
  const checkDigit = unitsDigit === 0 ? unitsDigit : (10 - (total % 10));
  return checkDigit.toString();
}

export function generateNPI(counter) {
  const paddedNumber = counter.toString().padStart(9, '0');
  return paddedNumber + generateLuhnCheckDigit(paddedNumber);
}

export default class NPIGenerator {
  constructor() {
    this.counter = 1;
  }

  iterate() {
    const npi = generateNPI(this.counter);
    counter++;
    return npi;
  }
}
