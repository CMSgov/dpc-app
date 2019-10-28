# frozen_string_literal: true

class FhirEndpoint < ApplicationRecord
  belongs_to :organization

  enum connection_type: {
    'hl7-fhir-rest' => 0,
    'ihe-xcpd' => 1,
    'ihe-xca' => 2,
    'ihe-xdr' => 3,
    'ihe-xds' => 4,
    'ihe-iid' => 5,
    'dicom-wado-rs' => 6,
    'dicom-qido-rs' => 7,
    'dicom-stow-r' => 8,
    'dicom-wado-uri' => 9,
    'hl7-fhir-msg' => 10,
    'hl7v2-mllp' => 11,
    'secure-email' => 12
  }

  enum status: {
    'test' => 0,
    'active' => 1,
    'suspended' => 2,
    'error' => 3,
    'off' => 4,
    'entered-in-error' => 5
  }
end
