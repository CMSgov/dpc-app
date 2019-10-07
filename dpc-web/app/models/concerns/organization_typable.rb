# frozen_string_literal: true

require 'active_support/concern'

module OrganizationTypable
  extend ActiveSupport::Concern

  ORGANIZATION_TYPES = {
    'primary_care_clinic' => 0,
    'speciality_clinic' => 1,
    'multispecialty_clinic' => 2,
    'inpatient_facility' => 3,
    'emergency_room' => 4,
    'urgent_care' => 5,
    'academic_facility' => 6,
    'health_it_vendor' => 7,
    'other' => 8
  }
end
