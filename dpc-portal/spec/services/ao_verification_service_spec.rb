# frozen_string_literal: true

require 'spec_helper'
require 'rails_helper'

describe AoVerificationService do
  let(:service) { AoVerificationService.new }
  let(:good_org_npi) { '3077494235' }
  let(:valid_ao_ssn) { '900111111' }
  let(:waived_ao_ssn) { '900777777' }
  let(:waived_org_npi) { '3098168743' }

  describe '.check_eligibility' do
    it 'succeeds with good input' do
      response = service.check_eligibility(good_org_npi, valid_ao_ssn)
      expect(response).to include({ success: true, has_ao_waiver: false, has_org_waiver: false })
      expect(response[:ao_role]).to be_present
    end

    it 'returns an error if looking up enrollments for the NPI returns a 404' do
      organization_npi = '3299073577'
      response = service.check_eligibility(organization_npi, valid_ao_ssn)
      expect(response).to include({ success: false, failure_reason: 'bad_npi' })
    end

    it 'returns an error if there are no approved enrollments' do
      organization_npi = '3782297014'
      response = service.check_eligibility(organization_npi, valid_ao_ssn)
      expect(response).to include({ success: false, failure_reason: 'no_approved_enrollment' })
    end

    it 'returns an error if the authorized official is only in an inactive enrollment' do
      not_ao_ssn = '900222222'
      response = service.check_eligibility(good_org_npi, not_ao_ssn)
      expect(response).to include({ success: false, failure_reason: 'user_not_authorized_official' })
    end

    it 'returns an error if the user is not an authorized official' do
      not_ao_ssn = '111223456'
      response = service.check_eligibility(good_org_npi, not_ao_ssn)
      expect(response).to include({ success: false, failure_reason: 'user_not_authorized_official' })
    end

    it 'returns an error if the AO has an active med sanction' do
      sanctioned_ao_ssn = '900666666'
      response = service.check_eligibility(good_org_npi, sanctioned_ao_ssn)
      expect(response).to include({ success: false, failure_reason: 'ao_med_sanctions' })
    end

    it 'returns an error if the org has an active med sanction' do
      sanctioned_org_npi = '3598564557'
      response = service.check_eligibility(sanctioned_org_npi, valid_ao_ssn)
      expect(response).to include({ success: false, failure_reason: 'org_med_sanctions' })
    end

    it 'does not return an error if user has a med sanction AND waiver' do
      response = service.check_eligibility(good_org_npi, waived_ao_ssn)
      expect(response).to include({ success: true, has_ao_waiver: true, has_org_waiver: false })
    end

    it 'does not return an error if org has a med sanction AND waiver' do
      response = service.check_eligibility(waived_org_npi, valid_ao_ssn)
      expect(response).to include({ success: true, has_ao_waiver: false, has_org_waiver: true })
    end

    it 'returns an error on Gateway server error' do
      error_generating_org_npi = '3593081045'
      response = service.check_eligibility(error_generating_org_npi, valid_ao_ssn)
      expect(response[:success]).to eq false
      expect(response[:failure_reason]).to eq 'api_gateway_error'
    end

    it 'returns an error on invalid endpoint' do
      invalid_endpoint_generating_org_npi = '3746980325'
      response = service.check_eligibility(invalid_endpoint_generating_org_npi, valid_ao_ssn)
      expect(response[:success]).to eq false
      expect(response[:failure_reason]).to eq 'invalid_endpoint_called'
    end

    it 'returns an error when any error' do
      unexpected_error_generating_org_npi = '3302763388'
      response = service.check_eligibility(unexpected_error_generating_org_npi, valid_ao_ssn)
      expect(response[:success]).to eq false
      expect(response[:failure_reason]).to eq 'unexpected_error'
    end
  end

  describe '.check_ao_eligibility' do
    it 'should work with good ssn' do
      response = service.check_ao_eligibility(good_org_npi, :ssn, valid_ao_ssn)
      expect(response).to include({ has_ao_waiver: false, has_org_waiver: false })
    end

    it 'should flag a user with a waiver' do
      response = service.check_ao_eligibility(good_org_npi, :ssn, waived_ao_ssn)
      expect(response).to include({ has_ao_waiver: true, has_org_waiver: false })
    end

    it 'should flag an org with a waiver' do
      response = service.check_ao_eligibility(waived_org_npi, :ssn, valid_ao_ssn)
      expect(response).to include({ has_ao_waiver: false, has_org_waiver: true })
    end

    it 'should raise error with bad ssn' do
      expect do
        service.check_ao_eligibility(good_org_npi, :ssn, 'not even ssn-like')
      end.to raise_error(AoException, 'user_not_authorized_official')
    end

    it 'should work with good pac_id' do
      expect do
        service.check_ao_eligibility(good_org_npi, :pac_id, 'validPacId')
      end.to_not raise_error
    end

    it 'should raise error with bad pac_id' do
      expect do
        service.check_ao_eligibility(good_org_npi, :pac_id, 'not there')
      end.to raise_error(AoException, 'user_not_authorized_official')
    end
  end
end
