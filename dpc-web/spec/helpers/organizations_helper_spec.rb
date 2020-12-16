# frozen_string_literal: true

require 'rails_helper'

RSpec.describe OrganizationsHelper, type: :helper do
  describe '#organization_types_for_select' do
    it 'formats types into an array with titleization' do
      expect(helper.organization_types_for_select).to eq(
        [
          ['Primary Care Clinic', 'primary_care_clinic'],
          ['Speciality Clinic', 'speciality_clinic'],
          ['Multispecialty Clinic', 'multispecialty_clinic'],
          ['Inpatient Facility', 'inpatient_facility'],
          ['Emergency Room', 'emergency_room'],
          ['Urgent Care', 'urgent_care'],
          ['Academic Facility', 'academic_facility'],
          ['Health IT Vendor', 'health_it_vendor'],
          ['Other', 'other']
        ]
      )
    end
  end
end
