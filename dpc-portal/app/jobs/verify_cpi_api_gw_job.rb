# frozen_string_literal: true

# A background job that verifies cpi gateway client can properly be used in dpc-portal
class VerifyCpiApiGwJob < ApplicationJob
  queue_as :portal

  def perform
    # let service handle API GW credentials and connection
    # this is what's used by both verify_ao_job and invitations already.
    service = AoVerificationService.new

    cpi_gateway_results = [
      can_process_ao_with_med_sanctions?,
      can_process_ao_with_waiver?,
      can_process_org_with_no_enrollment?,
      can_process_org_with_active_ao?
    ]

    trigger_alarm if !cpi_gateway_results.all?
  rescue StandardError => e
    log_failure(e)  # if we reach this, alarm should be caught by higher level checks (ie. unexpected errors check)
    raise
  end

  private
  def can_process_ao_with_med_sanctions?()
    # 1.) AO with med sanctions
    verification_result1 = service.check_eligibility(npi1, ssn1)
    return verification_result1[:success] == false && verification_result1[:has_ao_waiver] == false
  end

  def can_process_ao_with_waiver?()
    # 2.) AO with waivers
    verification_result2 = service.check_eligibility(npi2, ssn2)
    return verification_result2[:success] == true && verification_result2[:has_ao_waiver] == true
  end

  def can_process_org_with_no_enrollment?()
    # 3.) Not approved for enrollment
    service.get_approved_enrollments(npi3)
    false
  rescue AoException => e
    e.message == 'no_approved_enrollment'  # verify_provider_ogranization_job will call update_org_sanctions() here
  end

  def can_process_org_with_active_ao?()
    # 4.) Active AO
    enrollments_and_waivers = service.get_approved_enrollments(npi4)
    enrollments = enrollments_and_waivers[:enrollments]
    enrollments.present? && enrollments.any? do |enrollment|
      enrollment['roles'].is_a?(Array) &&
        enrollment['roles'].any? { |role| role['ssn'] == ssn4 }
    end
  rescue AoException => e
    false  # defensive, but should not get here
  end
end
