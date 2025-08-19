function getVUsCount() {
  if (__ENV.TEST_TYPE === 'single-iteration' || __ENV.TEST_TYPE === 'average-load-test') {
    return 3;
  } else if (__ENV.TEST_TYPE === 'stress-test') {
    return 5; 
  }
}

const constants = {
  preAllocatedVUs: 3,
  maxVUs: getVUsCount(),
}

export { constants };
