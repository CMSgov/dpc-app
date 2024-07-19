# frozen_string_literal: true

require 'spec_helper'
require 'rails_helper'

describe CpiApiGatewayClient do
  let(:client) { CpiApiGatewayClient.new }

  describe '.new' do
    it 'sets a token' do
      expect(client.access).to_not be_nil
    end
  end

  describe '.fetch_enrollment' do
    it 'returns enrollments' do
      enrollment = client.fetch_enrollment(12_345)
      expect(enrollment.dig('enrollments', 0, 'status')).to eq 'APPROVED'
    end

    it 'returns inactive enrollments with specific npi' do
      enrollment = client.fetch_enrollment('3782297014')
      expect(enrollment.dig('enrollments', 0, 'status')).to eq 'INACTIVE'
      expect(enrollment.dig('enrollments', 1, 'status')).to eq 'IN REVIEW'
    end
  end

  describe '.fetch_profile' do
    it 'returns enrollments' do
      enrollment = client.fetch_profile(12_345)
      expect(enrollment.dig('provider', 'enrollments').length).to eq 2
      expect(enrollment.dig('provider', 'enrollments', 0, 'status')).to eq 'INACTIVE'
      expect(enrollment.dig('provider', 'enrollments', 1, 'status')).to eq 'APPROVED'
    end

    it 'returns inactive enrollments with specific npi' do
      enrollment = client.fetch_profile('3782297014')
      expect(enrollment.dig('provider', 'enrollments').length).to eq 2
      expect(enrollment.dig('provider', 'enrollments', 0, 'status')).to eq 'INACTIVE'
      expect(enrollment.dig('provider', 'enrollments', 1, 'status')).to eq 'IN REVIEW'
    end

    it 'fetches roles' do
      roles = client.fetch_profile(12_345)
      expect(roles.dig('provider', 'enrollments', 0, 'roles', 0, 'roleCode')).to eq '10'
      expect(roles.dig('provider', 'enrollments', 0, 'roles', 0, 'ssn')).to eq '900222222'

      %w[900111111 900666666 900777777].each_with_index do |ssn, idx|
        expect(roles.dig('provider', 'enrollments', 1, 'roles', idx, 'roleCode')).to eq '10'
        expect(roles.dig('provider', 'enrollments', 1, 'roles', idx, 'ssn')).to eq ssn
      end
    end
  end

  describe '.fetch_authorized_official_med_sanctions' do
    it 'returns sanctions with specific ssn' do
      ssn = '900666666'
      sanctions = client.fetch_med_sanctions_and_waivers_by_ssn(ssn)
      expect(sanctions.dig('provider', 'medSanctions').size).to eq(1)
      expect(sanctions.dig('provider', 'waiverInfo').size).to eq(0)
    end

    it 'returns waiver with specific ssn' do
      ssn = '900777777'
      sanctions = client.fetch_med_sanctions_and_waivers_by_ssn(ssn)
      expect(sanctions.dig('provider', 'medSanctions').size).to eq(1)
      expect(sanctions.dig('provider', 'waiverInfo').size).to eq(1)
    end

    it 'does not return sanctions or waivers with other ssn' do
      ssn = '900121234'
      sanctions = client.fetch_med_sanctions_and_waivers_by_ssn(ssn)
      expect(sanctions.dig('provider', 'medSanctions').size).to eq(0)
      expect(sanctions.dig('provider', 'waiverInfo').size).to eq(0)
    end
  end

  describe '.fetch_org_med_sanctions' do
    it 'returns sanctions with specific npi' do
      npi = '3598564557'
      sanctions = client.fetch_med_sanctions_and_waivers_by_org_npi(npi)
      expect(sanctions.dig('provider', 'medSanctions').size).to eq(1)
      expect(sanctions.dig('provider', 'waiverInfo').size).to eq(0)
    end

    it 'returns waiver with specific npi' do
      npi = '3098168743'
      sanctions = client.fetch_med_sanctions_and_waivers_by_org_npi(npi)
      expect(sanctions.dig('provider', 'medSanctions').size).to eq(1)
      expect(sanctions.dig('provider', 'waiverInfo').size).to eq(1)
    end

    it 'does not return sanctions or waivers with other npi' do
      npi = '3740677877'
      sanctions = client.fetch_med_sanctions_and_waivers_by_org_npi(npi)
      expect(sanctions.dig('provider', 'medSanctions').size).to eq(0)
      expect(sanctions.dig('provider', 'waiverInfo').size).to eq(0)
    end
  end
end
