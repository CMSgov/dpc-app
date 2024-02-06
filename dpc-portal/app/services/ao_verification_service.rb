# frozen_string_literal: true

# A service that verifies a user as an Authorized Official (AO) for a given organization
class AOVerificationService
  def initialize
    @cpi_api_gw_client = CpiApiGatewayClient.new
  end

  def check_ao_eligibility(organization_npi, ao_ssn)
    # TODO: handle error cases
    # TODO: get address from enrollment
    enrollment_ids = get_approved_enrollments(organization_npi).map { |enrollment| enrollment['enrollmentID'] }
    ao_role = enrollment_ids.find { |enrollment_id| !get_authorized_official(enrollment_id, ao_ssn).nil? }
    return unless ao_role.nil?

    { success: false, reason: 'user_not_authorized_official' }
  end

  private

  def med_sanctions?(ao_ssn)
    # TODO: finish implementation
    @cpi_api_gw_client.fetch_authorized_official_med_sanctions(ao_ssn)
  end

  def get_approved_enrollments(organization_npi)
    response = @cpi_api_gw_client.fetch_enrollment(organization_npi)
    response['enrollments'].select { |enrollment| enrollment['status'] == 'APPROVED' }
  end

  def get_authorized_official(enrollment_id, ao_ssn)
    response = @cpi_api_gw_client.fetch_enrollment_roles(enrollment_id)
    response['enrollments']['roles'].find { |role| role['roleCode'] == '10' && role['ssn'] == ao_ssn.to_s }
  end
end
