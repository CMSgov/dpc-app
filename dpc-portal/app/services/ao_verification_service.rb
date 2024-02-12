# frozen_string_literal: true

# A service that verifies a user as an Authorized Official (AO) for a given organization
class AOVerificationService
  def initialize
    @cpi_api_gw_client = CpiApiGatewayClient.new
  end

  def check_ao_eligibility(organization_npi, hashed_ao_ssn)
    approved_enrollments = get_approved_enrollments(organization_npi)
    return { success: false, reason: approved_enrollments } if approved_enrollments == 'bad_npi'

    enrollment_ids = approved_enrollments.map { |enrollment| enrollment['enrollmentID'] }
    return { success: false, reason: 'no_approved_enrollment' } if enrollment_ids.empty?

    ao_role = nil
    enrollment_ids.find do |enrollment_id|
      ao_role = get_authorized_official_role(enrollment_id, hashed_ao_ssn)
    end
    return { success: false, reason: 'user_not_authorized_official' } if ao_role.nil?

    return { success: false, reason: 'med_sanctions' } if med_sanctions?(ao_role['ssn'])

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
