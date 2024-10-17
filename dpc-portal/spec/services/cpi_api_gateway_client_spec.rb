# frozen_string_literal: true

require 'spec_helper'
require 'rails_helper'

describe CpiApiGatewayClient do
  let(:client) { CpiApiGatewayClient.new }
  let(:host) { ENV.fetch('CPI_API_GW_BASE_URL', nil) }
  describe '.new' do
    it 'sets a token' do
      expect(client.access).to_not be_nil
    end
  end

  describe '.fetch_profile' do
    it 'returns enrollments' do
      verify_logs(status: 200, url: "#{host}api/1.0/ppr/providers/profile", method_name: :fetch_profile, method: :post)
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

    context 'fetches med sanctions' do
      it 'returns sanctions with specific npi' do
        npi = '3598564557'
        sanctions = client.fetch_profile(npi)
        expect(sanctions.dig('provider', 'medSanctions').size).to eq(1)
        expect(sanctions.dig('provider', 'waiverInfo')).to be_blank
      end

      it 'returns waiver with specific npi' do
        npi = '3098168743'
        sanctions = client.fetch_profile(npi)
        expect(sanctions.dig('provider', 'medSanctions').size).to eq(1)
        expect(sanctions.dig('provider', 'waiverInfo').size).to eq(1)
      end

      it 'does not return sanctions or waivers with other npi' do
        npi = '3740677877'
        sanctions = client.fetch_profile(npi)
        expect(sanctions.dig('provider', 'medSanctions')).to be_blank
        expect(sanctions.dig('provider', 'waiverInfo')).to be_blank
      end
    end
  end

  describe '.fetch_authorized_official_med_sanctions' do
    it 'returns sanctions with specific ssn' do
      verify_logs(status: 200, url: "#{host}api/1.0/ppr/providers", method_name: :fetch_provider_info,
                  method: :post)
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

  describe '.org_info' do
    it 'returns sanctions with specific npi' do
      verify_logs(status: 200, url: "#{host}api/1.0/ppr/providers", method_name: :fetch_provider_info, method: :post)
      npi = '3598564557'
      sanctions = client.org_info(npi)
      expect(sanctions.dig('provider', 'medSanctions').size).to eq(1)
      expect(sanctions.dig('provider', 'waiverInfo').size).to eq(0)
    end

    it 'returns waiver with specific npi' do
      npi = '3098168743'
      sanctions = client.org_info(npi)
      expect(sanctions.dig('provider', 'medSanctions').size).to eq(1)
      expect(sanctions.dig('provider', 'waiverInfo').size).to eq(1)
    end

    it 'does not return sanctions or waivers with other npi' do
      npi = '3740677877'
      sanctions = client.org_info(npi)
      expect(sanctions.dig('provider', 'medSanctions').size).to eq(0)
      expect(sanctions.dig('provider', 'waiverInfo').size).to eq(0)
    end
  end

  describe '.healthcheck' do
    it 'returns true when it can get a token' do
      expect(client.healthcheck).to eq(true)
    end

    it 'returns false when it cannot get a token' do
      expect(client.healthcheck).to eq(true)
    end
  end

  def verify_logs(status:, url:, method_name:, method: :get)
    verify_new_relic(url, method)
    verify_rails(status:, url:, method_name:, method:)
  end

  def verify_new_relic(uri, procedure)
    new_relic_tracer = instance_double(NewRelic::Agent::Transaction::ExternalRequestSegment)
    expect(NewRelic::Agent::Tracer).to receive(:start_external_request_segment)
      .with(library: 'Net::HTTP', uri:, procedure:)
      .and_return(new_relic_tracer)
    expect(new_relic_tracer).to receive(:finish)
  end

  def verify_rails(status:, url:, method_name:, method:)
    allow(Rails.logger).to receive(:info)
    expect(Rails.logger).to receive(:info).with(
      ['Calling CPI API Gateway',
       { cpi_api_gateway_request_method: method,
         cpi_api_gateway_request_url: url,
         cpi_api_gateway_request_method_name: method_name }]
    )
    expect(Rails.logger).to receive(:info).with(
      ['CPI API Gateway response info',
       { cpi_api_gateway_request_method: method,
         cpi_api_gateway_request_url: url,
         cpi_api_gateway_request_method_name: method_name,
         cpi_api_gateway_response_status_code: status,
         cpi_api_gateway_response_duration: anything }]
    )
  end
end
