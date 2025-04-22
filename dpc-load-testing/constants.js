function getVUsCount() {
  if (__ENV.TEST_TYPE === 'single-iteration' || __ENV.TEST_TYPE === 'average-load-test') {
    return 3;
  } else if (__ENV.TEST_TYPE === 'stress-test') {
    return 10; // This is a placeholder -- will do correct calculation as part of DPC-4633
  }
}

const constants = {
  preAllocatedVUs: 3,
  maxVUs: getVUsCount(),
}

export { constants };
