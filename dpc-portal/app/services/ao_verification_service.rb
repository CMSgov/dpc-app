# frozen_string_literal: true

# A service that verifies a user as an Authorized Official (AO) for a given organization, and verifies the organization
class AoVerificationService
  SERVER_ERRORS = %w[api_gateway_error invalid_endpoint_called unexpected_error].freeze

  def initialize
    @cpi_api_gw_client = CpiApiGatewayClient.new
  end

  # rubocop:disable Metrics/AbcSize
  def check_eligibility(organization_npi, ssn)
    response = check_ao_eligibility(organization_npi, :ssn, ssn)

    response.merge(success: true)
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
    role_and_waivers = get_authorized_official_role(organization_npi, identifier_type, identifier)
    individual_sanctions = check_individual_med_sanctions(role_and_waivers[:ao_role]['ssn'])

    role_and_waivers.merge(has_ao_waiver: individual_sanctions[:has_ao_waiver])
  end

  def get_approved_enrollments(organization_npi)
    profile = @cpi_api_gw_client.fetch_profile(organization_npi)
    raise AoException, 'bad_npi' if profile['code'] == '404'

    sanctions = check_sanctions_response(profile)
    raise AoException, 'org_med_sanctions' if sanctions[:is_sanctioned]

    enrollments = profile.dig('provider', 'enrollments')&.select { |enrollment| enrollment['status'] == 'APPROVED' }
    raise AoException, 'no_approved_enrollment' if enrollments.empty?

    { enrollments:, has_org_waiver: sanctions[:has_waiver] }
  end

  private

  def check_individual_med_sanctions(ao_ssn)
    response = @cpi_api_gw_client.fetch_med_sanctions_and_waivers_by_ssn(ao_ssn)
    sanctions = check_sanctions_response(response)
    raise AoException, 'ao_med_sanctions' if sanctions[:is_sanctioned]

    { is_sanctioned: sanctions[:is_sanctioned], has_ao_waiver: sanctions[:has_waiver] }
  end

  def check_sanctions_response(response)
    has_waiver = waiver?(response.dig('provider', 'waiverInfo'))
    return { is_sanctioned: false, has_waiver: true } if has_waiver

    med_sanctions_records = response.dig('provider', 'medSanctions')
    unless med_sanctions_records.nil? || med_sanctions_records.empty?
      current_med_sanction = med_sanctions_records.find do |record|
        record['reinstatementDate'].nil? || Date.parse(record['reinstatementDate']) > Date.today
      end
      return { is_sanctioned: true, has_waiver: false } if current_med_sanction.present?
    end

    { is_sanctioned: false, has_waiver: false }
  end

  def waiver?(waivers_list)
    return false unless waivers_list.present?

    active_waiver = waivers_list.find do |waiver|
      Date.parse(waiver['endDate']) > Date.today
    end
    !active_waiver.nil?
  end

  def get_authorized_official_role(organization_npi, identifier_type, identifier)
    profile = @cpi_api_gw_client.fetch_profile(organization_npi)
    raise AoException, 'bad_npi' if profile['code'] == '404'

    sanctions = check_sanctions_response(profile)
    raise AoException, 'org_med_sanctions' if sanctions[:is_sanctioned]

    enrollments = profile.dig('provider', 'enrollments')&.select { |enrollment| enrollment['status'] == 'APPROVED' }
    raise AoException, 'no_approved_enrollment' if enrollments.blank?

    { ao_role: role_from_enrollments(enrollments, identifier_type, identifier), has_org_waiver: sanctions[:has_waiver] }
  end

  def role_from_enrollments(enrollments, identifier_type, identifier)
    enrollments.each do |enrollment|
      enrollment['roles']&.each do |role|
        return role if role_matches(role, identifier_type, identifier)
      end
    end

    raise AoException, 'user_not_authorized_official'
  end

  def role_matches(role, identifier_type, identifier)
    case identifier_type
    when :ssn
      role['roleCode'] == '10' && role['ssn'] == identifier
    when :pac_id
      role['roleCode'] == '10' && role['pacId'] == identifier
    end
  end
end

class AoException < StandardError; end
