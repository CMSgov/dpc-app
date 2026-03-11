import exec from 'k6/execution'

export function isArrayUnique(arr) {
  return Array.isArray(arr) && new Set(arr).size === arr.length;
}

export function generateUniqueTestRunValue() {
  return `${exec.vu.idInInstance}+${exec.vu.iterationInInstance}+${Date.now()}`;
}