# frozen_string_literal: true

# A service that verifies a user as an Authorized Official (AO) for a given organization, and verifies the organization
class AoVerificationService
  attr_reader :cpi_api_gw_client

  def initialize
    @cpi_api_gw_client = CpiApiGatewayClient.new
  end

  # rubocop:disable Metrics/AbcSize
  def check_eligibility(organization_npi, hashed_ao_ssn)
    check_org_med_sanctions(organization_npi)
    check_ao_eligibility(organization_npi, :ssn, hashed_ao_ssn)

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
    Rails.logger.info "Failed check #{e.message} for organization NPI #{organization_npi}"
    { success: false, failure_reason: e.message }
  end
  # rubocop:enable Metrics/AbcSize

  def check_ao_eligibility(organization_npi, identifier_type, identifier)
    approved_enrollments = get_approved_enrollments(organization_npi)
    enrollment_ids = approved_enrollments.map { |enrollment| enrollment['enrollmentID'] }
    ao_role = get_authorized_official_role(enrollment_ids, identifier_type, identifier)
    check_provider_med_sanctions(ao_role['ssn'])
  end

  def check_org_med_sanctions(npi)
    response = @cpi_api_gw_client.fetch_med_sanctions_and_waivers_by_org_npi(npi)
    raise AoException, 'org_med_sanctions' if check_sanctions_response(response)
  end

  def get_approved_enrollments(organization_npi)
    response = @cpi_api_gw_client.fetch_enrollment(organization_npi)
    raise AoException, 'bad_npi' if response['code'] == '404'

    enrollments = response['enrollments'].select { |enrollment| enrollment['status'] == 'APPROVED' }
    raise AoException, 'no_approved_enrollment' if enrollments.empty?

    enrollments
  end

  private

  def check_provider_med_sanctions(ao_ssn)
    response = @cpi_api_gw_client.fetch_med_sanctions_and_waivers_by_ssn(ao_ssn)
    raise AoException, 'ao_med_sanctions' if check_sanctions_response(response)
  end

  def check_sanctions_response(response)
    return false if waiver?(response.dig('provider', 'waiverInfo'))

    med_sanctions_records = response.dig('provider', 'medSanctions')
    unless med_sanctions_records.nil? || med_sanctions_records.empty?
      current_med_sanction = med_sanctions_records.find do |record|
        record['reinstatementDate'].nil? || Date.parse(record['reinstatementDate']) > Date.today
      end
      return true if current_med_sanction.present?
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

  def get_authorized_official_role(enrollment_ids, identifier_type, identifier)
    enrollment_ids.each do |enrollment_id|
      response = @cpi_api_gw_client.fetch_enrollment_roles(enrollment_id)
      roles_response = response.dig('enrollments', 'roles')
      roles_response.each do |role|
        return role if role_matches(role, identifier_type, identifier)
      end
    end

    raise AoException, 'user_not_authorized_official'
  end

  def role_matches(role, identifier_type, identifier)
    case identifier_type
    when :ssn
      role['roleCode'] == '10' && Digest::SHA2.new(256).hexdigest(role['ssn']) == identifier
    when :pac_id
      role['roleCode'] == '10' && role['pacId'] == identifier
    end
  end
end

class AoException < StandardError; end
