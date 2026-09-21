# frozen_string_literal: true

# A background job that verifies cpi gateway client can properly be used in dpc-portal
class VerifyCpiApiGwJob < ApplicationJob
  queue_as :portal

  # rubocop:disable-next Metrics/AbcSize
  def perform
    # CPI GW overrides so this job can use IPML and rest of portal can use fake_cpi_gateway.rb
    cpi_client_config = configuration
    cpi_api_gw_client = CpiApiGatewayClient.new(cpi_client_config['OAUTH_URL'], cpi_client_config['BASE_URL'])

    # let service handle API GW credentials and connection
    # this is what's used by both verify_ao_job and invitations already.
    service = AoVerificationService.new(cpi_api_gw_client:)
    test_scenarios = test_data

    cpi_gateway_results = [
      can_process_ao_with_med_sanctions?(service, test_scenarios['AO_WITH_MED_SANCTIONS']),
      can_process_ao_with_waiver?(service, test_scenarios['AO_WITH_WAIVERS']),
      can_process_org_with_no_enrollment?(service, test_scenarios['UNAPPROVED_ENROLLMENT_STATUS']),
      can_process_org_with_active_ao?(service, test_scenarios['ORG_WITH_AO_SSN'])
    ]

    Rails.logger.info "CPI API gateway  verification tests: #{cpi_gateway_results.count(true)} out of #{cpi_gateway_results.length} tests passing"
    # add log that cpi_gateway_results.length organizations processed
    trigger_alarm unless cpi_gateway_results.all?
  rescue JSON::ParserError, KeyError => e
    Rails.logger.error "failed to process JSON: #{e}"
  rescue StandardError => e
    Rails.logger.error "unexpected error: #{e}"
    raise
  end

  private

  def can_process_ao_with_med_sanctions?(service, test_data)
    # VerifyAoJob path
    check_ao_eligibility_result = check_ao_eligibility_safe(service, test_data)
    return false unless check_ao_eligibility_result[:error_message] == 'ao_med_sanctions'

    # VerifyProviderOrganizationJob path
    approved_enrollments_result = get_approved_enrollments_safe(service, test_data)
    return false unless approved_enrollments_result[:error_message].nil?

    # Invitation path
    result = service.check_eligibility(test_data['org_npi'], test_data['ao_ssn'])
    result[:success] == false && result[:failure_reason] == 'ao_med_sanctions'
  end

  def can_process_ao_with_waiver?(service, test_data)
    # VerifyAoJob path
    check_ao_eligibility_result = check_ao_eligibility_safe(service, test_data)
    return false unless check_ao_eligibility_result[:result][:has_ao_waiver] == true

    # VerifyProviderOrganizationJob path
    enrollments = get_approved_enrollments_safe(service, test_data)[:enrollments]
    return false unless enrollments.present?
    return false unless enrollments_has_ssn(enrollments, test_data['ao_ssn'])

    # Invitation path
    result = service.check_eligibility(test_data['org_npi'], test_data['ao_ssn'])
    result[:success] == true && result[:has_ao_waiver] == true
  end

  def can_process_org_with_no_enrollment?(service, test_data)
    # VerifyAoJob path
    check_ao_eligibility_result = check_ao_eligibility_safe(service, test_data)
    return false unless check_ao_eligibility_result[:error_message] == 'no_approved_enrollment'

    # VerifyProviderOrganizationJob path
    approved_enrollments_result = get_approved_enrollments_safe(service, test_data)
    return false unless approved_enrollments_result[:error_message] == 'no_approved_enrollment'

    # Invitation path
    result = service.check_eligibility(test_data['org_npi'], test_data['ao_ssn'])
    result[:success] == false && result[:failure_reason] == 'no_approved_enrollment'
  end

  def can_process_org_with_active_ao?(service, test_data)
    # VerifyAoJob path
    check_ao_eligibility_result = check_ao_eligibility_safe(service, test_data)
    return false unless check_ao_eligibility_result[:result][:has_org_waiver] == false
    return false unless check_ao_eligibility_result[:result][:has_ao_waiver] == false

    # VerifyProviderOrganizationJob path
    enrollments = get_approved_enrollments_safe(service, test_data)[:enrollments]
    return false unless enrollments.present?
    return false unless enrollments_has_ssn(enrollments, test_data['ao_ssn'])

    # Invitation path
    result = service.check_eligibility(test_data['org_npi'], test_data['ao_ssn'])
    result[:success] == true
  end

  # Happy path for this method returns an object with an an array under enrollments.
  # When there are no approved enrollments, an exception is raised.
  def get_approved_enrollments_safe(service, test_data)
    result = service.get_approved_enrollments(test_data['org_npi'])
    { enrollments: result[:enrollments], error_message: nil }
  rescue AoException => e
    { enrollments: nil, error_message: e.message }
  end

  # There are two things we care about here: has_ao_waiver and has_org_waiver
  def check_ao_eligibility_safe(service, test_data)
    result = service.check_ao_eligibility(test_data['org_npi'], :pac_id, test_data['ao_pac_id'])
    { result:, error_message: nil }
  rescue AoException => e
    { result: nil, error_message: e.message }
  end

  def enrollments_has_ssn?(enrollments_arr, ssn)
    enrollments_arr.any? do |enrollment|
      enrollment['roles'].is_a?(Array) &&
        enrollment['roles'].any? { |role| role['ssn'] == ssn }
    end
  end

  def test_data
    # retrieve from /dpc/test/web-portal/cpi_api_gw_testdata etc
    # includes 4 specific test scenarios set up by the CPI API GW team
    testdata_object = JSON.parse(ENV.fetch('CPI_API_GW_TESTDATA'))
    testdata_object.fetch('data')
  end

  def configuration
    # retrieve from /dpc/test/web-portal/cpi_api_gw_testdata etc
    # includes "BASE_URL" and "OAUTH_URL"
    # "TOKEN_ENDPOINT" should be same as what's already used
    testdata_object = JSON.parse(ENV.fetch('CPI_API_GW_TESTDATA'))

    # might need to change "test" to ENV.fetch('ENV')
    testdata_object.fetch('meta').fetch('configuration').fetch('test')
  end
end
