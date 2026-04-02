export function isArrayUnique(arr) {
  return Array.isArray(arr) && new Set(arr).size === arr.length;
}

export function isEmptyObject(obj) {
  return obj && Object.keys(obj).length === 0 && obj.constructor === Object;
}

export function isObjectType(obj, key) {
    return obj && obj[key] && typeof obj[key] === 'object' && !Array.isArray(obj[key]);
}

export function isArrayType(obj, key) {
    return obj && obj[key] && Array.isArray(obj[key]);
}

export function isDate(dateString) {
    const date = Date.parse(dateString);
    return !isNaN(date);
}
