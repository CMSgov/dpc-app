export function isArrayUnique(arr) {
  return Array.isArray(arr) && new Set(arr).size === arr.length;
}

export const fhirType = 'application/fhir+json';

export const fhirOK = function(res) {
  return (res.status === 200 || res.status === 201) && res.headers['Content-Type'] === fhirType;
};

export const getUuidFromUrl = (s) => { // e.g.
  const m = s.match(/\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\b(?=\/?$)/i);
  return m ? m[0] : null;
};

export const memberContentVerified = function(res, patients) {
  let pass = true;
  res.json().member.forEach((patient) => {
    if (!patients.includes(patient.entity.reference.slice(8))){
      pass = false;
    }
    if (!patient.period.start || patient.period.start === patient.period.end) {
      pass = false;
    }
  });
  return pass;
}
