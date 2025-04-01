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
  constructor(counter=0) {
    this.counter = counter;
  }

  iterate() {
    const mbi = generateMBI(this.counter);
    this.counter++;
    return mbi;
  }
}

/**
 * A simple cache to store instance of MBIGenerator, to be used across iterations 
 * for each workflow.
 * 
 * Generators need to be instantiated within the scope of each workflow. However,
 * simply calling MBIGenerator.instantiate within the scope of a workflow will cause a new
 * instance to be created on every iteration. 
 * 
 * Instantiated instance reserves 10000 unique identifiers.
 * 
 */
export class MBIGeneratorCache {
  constructor() {
    this.generators = {};
  }

  getGenerator(vuId) {
    if (vuId === undefined) {
      exec.test.abort('No VU passed to MBIGenerator; aborting test.');
    }
    if (this.generators[vuId] === undefined) {
      this.generators[vuId] = new MBIGenerator(vuId * 10000);
    }
    
    return this.generators[vuId];
  }
}
