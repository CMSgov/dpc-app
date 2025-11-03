export function isArrayUnique(arr) {
  return Array.isArray(arr) && new Set(arr).size === arr.length;
}

export const fhirType = 'application/fhir+json';

export const fhirOK = function(res) {
  return (res.status === 200 || res.status === 201) && res.headers['Content-Type'] === fhirType;
};

// case insensitive match header e.g. "Content-Location" or "content-location"
export function getHeader(res, headerName) {
  if (!res || !res.headers) {
    return '';
  }
  for (const name in res.headers) {
    if (name.toLowerCase() === headerName.toLowerCase()) return res.headers[name];
  }
  return '';
}

export const getUuidFromUrl = (s) => { // e.g.
  const m = s.match(/\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\b(?=\/?$)/i);
  return m ? m[0] : null;
};
