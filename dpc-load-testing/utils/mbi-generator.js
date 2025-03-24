// These letters are not allowed in valid MBIs. We don't want to accidentally test on real peoples' MBIs, so we'll use these to generate fakes.
const alpha = 'BILOSZ'; 
const numericFrom1 = '123456789';
const numericFrom0 = '0123456789';
const alphaNumeric = alpha + numericFrom0;

// the set of characters allowed at each index
const allowedCharacters = [
  numericFrom1, 
  alpha,
  alphaNumeric,
  numericFrom0,
  alpha,
  alphaNumeric,
  numericFrom0,
  alpha,
  alpha,
  numericFrom0,
  numericFrom0
];

export function generateMBI(counter) {
  let n = counter;
  let output = '';
  for (let i = 10; i >= 0; i--) {
    const base = allowedCharacters[i].length;
    const index = n % base;
    output = allowedCharacters[i][index] + output;
    n = Math.floor(n / base);
  }

  return output;
}

export default class MBIGenerator {
  constructor() {
    this.counter = 0;
  }

  iterate() {
    const mbi = generateMBI(this.counter);
    this.counter++;
    return mbi;
  }
}

// function test() {
//   const gen = new MBIGenerator();
//   for (let i = 0; i <= 20; i++) {
//     console.log(gen.iterate());
//   }
// }

// test();
