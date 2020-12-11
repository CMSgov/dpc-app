# frozen_string_literal: true

require 'luhnacy_lib'
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

  describe '#generate_npi' do
    it 'generates a valid npi' do
      npi1 = helper.generate_npi
      npi2 = helper.generate_npi
      npi3 = helper.generate_npi
      # binding.pry
      npi1_check = LuhnacyLib.validate_npi('80840' + npi1)
      npi2_check = LuhnacyLib.validate_npi('80840' + npi2)
      npi3_check = LuhnacyLib.validate_npi('80840' + npi3)

      expect(npi1_check).to eq(true)
      expect(npi2_check).to eq(true)
      expect(npi3_check).to eq(true)
    end
  end
end
