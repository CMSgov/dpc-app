# frozen_string_literal: true

# A service that verifies a user as an Authorized Official (AO) for a given organization
class AOVerificationService
  def initialize
    @cpi_api_gw_client = CpiApiGatewayClient.new
  end

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

      ao_role = nil
      enrollment_ids.find { |enrollment_id| ao_role = get_authorized_official_role(enrollment_id, hashed_ao_ssn) }
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
        return { success: false, reason: 'api_gateway_error' }
      elsif e.response.status == 404
        return { success: false, reason: 'invalid_endpoint_called' }
      end
    end

    { success: true }
  end

  private

  def med_sanctions?(ao_ssn)
    response = @cpi_api_gw_client.fetch_authorized_official_med_sanctions(ao_ssn)
    med_sanctions_records = response['provider']['medSanctions']
    if med_sanctions_records.nil? || med_sanctions_records.empty?
      false
    else
      current_med_sanction = med_sanctions_records.find do |record|
        record['reinstatementDate'].nil? || Date.parse(record['reinstatementDate']) > Date.today
      end
      !current_med_sanction.nil?
    end
  end

  def get_approved_enrollments(organization_npi)
    response = @cpi_api_gw_client.fetch_enrollment(organization_npi)
    return 'bad_npi' if response['code'] == '404'

    response['enrollments'].select { |enrollment| enrollment['status'] == 'APPROVED' }
  end

  def get_authorized_official_role(enrollment_id, hashed_ao_ssn)
    response = @cpi_api_gw_client.fetch_enrollment_roles(enrollment_id)
    response['enrollments']['roles'].find do |role|
      role['roleCode'] == '10' && Digest::SHA2.new(256).hexdigest(role['ssn']) == hashed_ao_ssn
    end
  end
end
