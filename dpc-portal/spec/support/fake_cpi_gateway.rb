# frozen_string_literal: true

require 'sinatra/base'
require './lib/tasks/npis'

# rubocop:disable Metrics/ClassLength
# This class simulates the CPI API Gateway and the IDM identity service
#
# Endpoints:
#   Provider
#    900666666: ssn with med sanctions
#    900777777: ssn with waived med sanctions
#    3598564557: org npi with med sanctions
#    3098168743: org npi with waived med sanctions
#   Enrollments
#    Most npis return valid enrollments, but the following will cause errors
#    3299073577: returns code 404
#    3782297014: has no valid enrollments
#   Enrollment Roles
#    AO SSNS: 900111111
class FakeCpiGateway < Sinatra::Base
  set :server, 'thin'

  set(:method) do |method|
    method = method.to_s.upcase
    condition { request.request_method == method }
  end

  before method: :post do
    if request.env['CONTENT_TYPE'] == 'application/json'
      request.body.rewind
      @request_payload = JSON.parse request.body.read
    end
  end

  # IDM identity service
  post '/oauth2/:token_id/v1/token' do
    headers['content-type'] = 'application/json; charset=UTF-8'
    {
      access_token: 'fake-token',
      token_type: 'Bearer',
      expires_in: 3600,
      refresh_token: 'fake-token',
      scope: 'create'
    }.to_json
  end

  # CPI API Gateway
  # Enrollments
  post '/api/1.0/ppr/providers/enrollments' do
    headers['content-type'] = 'application/json; charset=UTF-8'
    npi = @request_payload.dig('providerID', 'npi')
    case npi
    when '3593081045'
      halt 500, {
        success: 'false',
        response: 'Internal server error'
      }.to_json
    when '3746980325'
      halt 404, {
        success: 'false',
        response: 'no such path'
      }.to_json
    when '3302763388'
      halt 430, {
        success: 'false',
        response: 'Shopify Security Rejection'
      }.to_json
    when '3299073577'
      '{"code": "404"}'
    when *Npis::ORG_NO_ENROLLMENTS
      {
        enrollments: [
          { status: 'INACTIVE' },
          { status: 'IN REVIEW' }
        ]
      }.to_json
    else
      {
        enrollments: [
          {
            status: 'APPROVED',
            enrollmentID: npi
          }
        ]
      }.to_json
    end
  end

  # Enrollment Roles
  get '/api/1.0/ppr/providers/enrollments/:enrollmentID/roles' do
    headers['content-type'] = 'application/json; charset=UTF-8'
    ao_ssns = %w[900111111 900666666 900777777 900888888 666222222]
    roles = ao_ssns.map { |ssn| { pacId: ssn, roleCode: '10', ssn: } }
    roles << { pacId: 'validPacId', roleCode: '10', ssn: '900428421' }
    PacIds::AO_HAS_ROLE.each do |pac_id|
      roles << { pacId: pac_id, roleCode: '10', ssn: pac_id }
    end
    {
      enrollments: {
        roles:
      }
    }.to_json
  end

  # Providers (med sanctions)
  post '/api/1.0/ppr/providers' do
    headers['content-type'] = 'application/json; charset=UTF-8'
    provider_type = @request_payload.dig('providerID', 'providerType')
    identifier = if provider_type == 'ind'
                   @request_payload.dig('providerID', 'identity', 'id')
                 else
                   @request_payload.dig('providerID', 'npi')
                 end

    return '{"code": "404"}' if identifier == '3299073577'

    provider = { providerType: provider_type, medSanctions: [], waiverInfo: [] }

    provider[:orgName] = "Organization #{identifier}" if provider_type == 'org'
    sanctioned_ids = %w[900666666 900777777 3598564557 3098168743]
    sanctioned_ids += Npis::ORG_FAILS_MED_CHECK
    sanctioned_ids += PacIds::AO_FAILS_MED_CHECK

    if sanctioned_ids.include?(identifier)
      provider[:medSanctions] << {
        sanctionCode: '12ABC',
        sanctionDate: '2010-08-17',
        description: 'MED Sanction',
        deletedTimestamp: '2021-09-24T16:26:48.598-04:00',
        reinstatementDate: nil,
        reinstatementReasonDescription: 'LICENSE REVOCATION OR SUSPENSION'
      }
    end

    if %w[900777777 3098168743].include?(identifier)
      provider[:waiverInfo] << {
        effectiveDate: Date.today.prev_year.to_s,
        endDate: Date.today.next_year.to_s,
        comment: 'Waiver covers everything'
      }
    end

    { provider: }.to_json
  end

  run! if __FILE__ == $PROGRAM_NAME
end
# rubocop:enable Metrics/ClassLength
