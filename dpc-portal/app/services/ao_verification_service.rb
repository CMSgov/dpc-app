# frozen_string_literal: true

# A service that verifies a user as an Authorized Official (AO) for a given organization
class AoVerificationService
  def initialize
    @cpi_api_gw_client = CpiApiGatewayClient.new
  end

  # rubocop:disable Metrics/AbcSize
  # rubocop:disable Metrics/CyclomaticComplexity
  # rubocop:disable Metrics/MethodLength
  # rubocop:disable Metrics/PerceivedComplexity
  def check_ao_eligibility(organization_npi, hashed_ao_ssn)
    begin
      approved_enrollments = get_approved_enrollments(organization_npi)
      if approved_enrollments == 'bad_npi'
        Rails.logger.warn "Unable to find organization NPI #{organization_npi}"
        return { success: false, reason: approved_enrollments }
      end

      enrollment_ids = approved_enrollments.map { |enrollment| enrollment['enrollmentID'] }
      if enrollment_ids.empty?
        Rails.logger.warn "No current approved enrollments for organization NPI #{organization_npi}"
        return { success: false, reason: 'no_approved_enrollment' }
      end

      ao_role = get_authorized_official_role(enrollment_ids, hashed_ao_ssn)
      if ao_role.nil?
        Rails.logger.warn "User failed Authorized Official status for organization NPI #{organization_npi}"
        return { success: false, reason: 'user_not_authorized_official' }
      end

      if med_sanctions?(ao_role['ssn'])
        Rails.logger.warn "User attempting to authorize for organization NPI #{organization_npi} has med sanctions."
        return { success: false, reason: 'med_sanctions' }
      end
    rescue OAuth2::Error => e
      if e.response.status == 500
        Rails.logger.error 'API Gateway Error during AO Verification'
        return { success: false, reason: 'api_gateway_error' }
      elsif e.response.status == 404
        Rails.logger.error 'Invalid API Gateway endpoint called during AO verification'
        return { success: false, reason: 'invalid_endpoint_called' }
      end
    end

    { success: true }
  end
  # rubocop:enable Metrics/AbcSize
  # rubocop:enable Metrics/CyclomaticComplexity
  # rubocop:enable Metrics/MethodLength
  # rubocop:enable Metrics/PerceivedComplexity

  private

  def med_sanctions?(ao_ssn)
    response = @cpi_api_gw_client.fetch_med_sanctions_and_waivers(ao_ssn)
    return false if waiver?(response.dig('provider', 'waiverInfo'))

    med_sanctions_records = response.dig('provider', 'medSanctions')
    if med_sanctions_records.nil? || med_sanctions_records.empty?
      false
    else
      current_med_sanction = med_sanctions_records.find do |record|
        record['reinstatementDate'].nil? || Date.parse(record['reinstatementDate']) > Date.today
      end
      current_med_sanction.present?
    end
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
    return 'bad_npi' if response['code'] == '404'

    response['enrollments'].select { |enrollment| enrollment['status'] == 'APPROVED' }
  end

  def get_authorized_official_role(enrollment_ids, hashed_ao_ssn)
    enrollment_ids.each do |enrollment_id|
      response = @cpi_api_gw_client.fetch_enrollment_roles(enrollment_id)
      roles_response = response.dig('enrollments', 'roles')
      roles_response.each do |role|
        return role if role['roleCode'] == '10' && Digest::SHA2.new(256).hexdigest(role['ssn']) == hashed_ao_ssn
      end
    end

    nil
  end
end
