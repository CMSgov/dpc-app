# frozen_string_literal: true

# A service that verifies a user as an Authorized Official (AO) for a given organization
class AoVerificationService
  def initialize
    @cpi_api_gw_client = CpiApiGatewayClient.new
  end

  # rubocop:disable Metrics/AbcSize
  def check_ao_eligibility(organization_npi, hashed_ao_ssn)
    approved_enrollments = get_approved_enrollments(organization_npi)
    enrollment_ids = approved_enrollments.map { |enrollment| enrollment['enrollmentID'] }
    ao_role = get_authorized_official_role(enrollment_ids, hashed_ao_ssn)
    check_med_sanctions(ao_role['ssn'])

    { success: true }
  rescue OAuth2::Error => e
    if e.response.status == 500
      Rails.logger.error 'API Gateway Error during AO Verification'
      { success: false, failure_reason: 'api_gateway_error' }
    elsif e.response.status == 404
      Rails.logger.error 'Invalid API Gateway endpoint called during AO verification'
      { success: false, failure_reason: 'invalid_endpoint_called' }
    else
      Rails.logger.error 'Unexpected error during AO Verification'
      { success: false, failure_reason: 'unexpected_error' }
    end
  rescue AoException => e
    Rails.logger.info "Failed AO check #{e.message} for organization NPI #{organization_npi}"
    { success: false, failure_reason: e.message }
  end
  # rubocop:enable Metrics/AbcSize

  private

  def check_med_sanctions(ao_ssn)
    response = @cpi_api_gw_client.fetch_med_sanctions_and_waivers(ao_ssn)
    return false if waiver?(response.dig('provider', 'waiverInfo'))

    med_sanctions_records = response.dig('provider', 'medSanctions')
    unless med_sanctions_records.nil? || med_sanctions_records.empty?
      current_med_sanction = med_sanctions_records.find do |record|
        record['reinstatementDate'].nil? || Date.parse(record['reinstatementDate']) > Date.today
      end
      raise AoException, 'med_sanctions' if current_med_sanction.present?
    end

    false
  end

  def waiver?(waivers_list)
    return false unless waivers_list.present?

    active_waiver = waivers_list.find do |waiver|
      Date.parse(waiver['endDate']) > Date.today
    end
    !active_waiver.nil?
  end

  def get_approved_enrollments(organization_npi)
    response = @cpi_api_gw_client.fetch_enrollment(organization_npi)
    raise AoException, 'bad_npi' if response['code'] == '404'

    enrollments = response['enrollments'].select { |enrollment| enrollment['status'] == 'APPROVED' }
    raise AoException, 'no_approved_enrollment' if enrollments.empty?

    enrollments
  end

  def get_authorized_official_role(enrollment_ids, hashed_ao_ssn)
    enrollment_ids.each do |enrollment_id|
      response = @cpi_api_gw_client.fetch_enrollment_roles(enrollment_id)
      roles_response = response.dig('enrollments', 'roles')
      roles_response.each do |role|
        return role if role['roleCode'] == '10' && Digest::SHA2.new(256).hexdigest(role['ssn']) == hashed_ao_ssn
      end
    end

    raise AoException, 'user_not_authorized_official'
  end
end

class AoException < StandardError; end
