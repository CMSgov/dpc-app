import luhn from "../lib/luhn.js";

export function generateNPI(counter) {
  const paddedNumber = counter.toString().padStart(9, '0');
  return luhn.generate('80840' + paddedNumber).slice(5);
}

export class NPIGenerator {
  constructor(counter=1) {
    this.counter = counter;
  }

  iterate() {
    const npi = generateNPI(this.counter);
    this.counter++;
    return npi;
  }
}

/**
 * A simple cache to store instance of NPIGenerator, to be used across iterations 
 * for each workflow.
 * 
 * Generators need to be instantiated within the scope of each workflow. However,
 * simply calling NPIGenerator.instantiate within the scope of a workflow will cause a new
 * instance to be created on every iteration. 
 * 
 * Instantiated instance reserves 10000 unique identifiers.
 * 
 */
export default class NPIGeneratorCache {
  constructor() {
    this.generators = {};
  }

  getGenerator(vuId) {
    if (vuId === undefined) {
      throw new Error('No VU passed to NPIGenerator; aborting test.');
    }
    if (this.generators[vuId] === undefined) {
      this.generators[vuId] = new NPIGenerator(vuId * 10000);
    }
    
    return this.generators[vuId];
  }
}
