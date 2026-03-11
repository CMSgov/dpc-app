export function isArrayUnique(arr) {
  return Array.isArray(arr) && new Set(arr).size === arr.length;
}
