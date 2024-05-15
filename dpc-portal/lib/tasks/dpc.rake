# frozen_string_literal: true

require './vendor/api_client/app/services/dpc_client'
require './vendor/api_client/app/serializers/organization_submit_serializer'
require './lib/tasks/npis'

namespace :dpc do
  desc 'Cleans up Performance tests Verification jobs'
  task cleanup_perf: :environment do
    cleanup
  end
  desc 'Performance tests Verification jobs'
  task verify_perf: :environment do
    only_good = ENV.fetch('GOOD', false)
    ENV['VERIFICATION_MAX_RECORDS'] = '10000'
    if only_good
      Npis::GOOD.each do |npi|
        make_basic(npi, PacIds::AO_GOOD, 'Good')
      end
    else
      @good_npis = Npis::GOOD[...400].dup
      build_models
    end
    puts 'Starting Jobs'
    @start = Time.now
    puts @start
    VerifyAoJob.perform_now
    puts "VerifyAoJob took #{(Time.now - @start).floor(1)} seconds"
    if only_good
      verify_only_good_ao_job
    else
      verify_ao_job
      @start = Time.now
      VerifyProviderOrganizationJob.perform_now
      puts "VerifyProviderOrganizationJob took #{(Time.now - @start).floor(1)} seconds"
      verify_org_job
    end
    cleanup unless ENV.fetch('NO_CLEANUP', false)
  end

  desc <<~DESC
    Create an Invitation for an Authorized Official
    provide comma-separated values in INVITE: given name, family name, email, org npi
    e.g. rails dpc:invite_ao INVITE=Bob,Hoskins,bob@example.com,11111111111
  DESC
  task invite_ao: :environment do
    ao_given_name, ao_family_name, ao_email, org_npi = ENV['INVITE'].split(',')
    service = AoInvitationService.new
    invitation = service.create_invitation(ao_given_name, ao_family_name, ao_email, org_npi)
    puts "Invitation created for #{ao_given_name} #{ao_family_name} for #{invitation.provider_organization.name}"
    if Rails.env.development?
      puts "http://localhost:3100/portal/organizations/#{invitation.provider_organization.id}/invitations/#{invitation.id}/accept"
    end
  rescue AoInvitationServiceError => e
    puts "Unable to create invitation: #{e.message}"
  end

  desc 'Create a new organization with random attributes. Prints link.'
  task :make_org do
    raise BadEnvironmentError, 'Only for local development' unless ENV['ENV'] == 'local'

    org = MockOrg.new
    fhir_endpoint = { 'status' => 'test',
                      'name' => 'DPC Sandbox Test Endpoint',
                      'uri' => 'https://dpc.cms.gov/test-endpoint' }
    client = DpcClient.new
    client.create_organization(org, fhir_endpoint:)
    if client.response_successful?
      puts "http://localhost:3100/portal/organizations/#{client.response_body['id']}"
    else
      puts "HTTP ERROR #{client.response_status} creating org"
    end
  end
end

def passes_by_org(npis, org_pass, link_pass, user_pass, sanction, debug = false)
  return :fail if npis.empty?

  npis.each do |npi|
    org = ProviderOrganization.find_by_npi(npi)
    puts "ORG: #{org.inspect}" if debug
    return :fail_org_last_checked if !org_pass && org.last_checked_at < @start

    if org_pass
      return :fail_org_pass if org.verification_status == 'rejected' || org.verification_reason.present?
    elsif org.verification_status != 'rejected' || org.verification_reason != sanction
      return :fail_org_fail
    end

    link = org.ao_org_links.first
    continue unless link

    puts "LINK: #{link.inspect}" if debug
    return :fail_link_last_checked if link.last_checked_at < @start

    if link_pass
      return :fail_link_pass if !link.verification_status || link.verification_reason.present?
    elsif link.verification_status || link.verification_reason != sanction
      return :fail_link_fail
    end

    puts "USER: #{link.user.inspect}" if debug
    return :fail_user_last_checked if !user_pass && link.user.last_checked_at < @start

    if user_pass
      return :fail_user_pass if link.user.verification_status == 'rejected' || link.user.verification_reason.present?
    elsif link.user.verification_status != 'rejected' || link.user.verification_reason != sanction
      return :fail_user_fail
    end
  end
  :success
end

def verify_ao_job
  puts "ORG MED CHECK     : #{passes_by_org(Npis::ORG_FAILS_MED_CHECK, false, false, true, 'org_med_sanctions')}"
  puts "ORG NO ENROLLMENTS: #{passes_by_org(Npis::ORG_NO_ENROLLMENTS, false, false, true, 'no_approved_enrollment')}"
  puts "AO NO LONGER AO   : #{passes_by_org(Npis::AO_NO_LONGER_AO, true, false, true, 'user_not_authorized_official')}"
  puts "AO FAILS MED CHECK: #{passes_by_org(Npis::AO_FAILS_MED_CHECK, false, false, false, 'ao_med_sanctions')}"
  puts "GOOD ORGS         : #{passes_by_org(Npis::GOOD[...400], true, true, true, '')}"
end

def verify_only_good_ao_job
  puts "GOOD ORGS         : #{passes_by_org(Npis::GOOD, true, true, true, '')}"
end

def verify_org_job
  no_ao_fail = false
  ProviderOrganization.where(npi: Npis::NO_AO).each do |org|
    if org.last_checked_at < @start
      no_ao_fail = true
      break
    end
  end
  if no_ao_fail
    puts 'NO AO             : fail'
  else
    puts 'NO AO             : success'
  end
end

def build_models
  Npis::ORG_FAILS_MED_CHECK.each do |npi|
    make_basic(npi, PacIds::AO_GOOD, 'Fail Med Check')
  end
  Npis::ORG_NO_ENROLLMENTS.each do |npi|
    make_basic(npi, PacIds::AO_GOOD, 'No Enrollments')
  end
  Npis::AO_NO_LONGER_AO.each do |npi|
    make_basic(npi, PacIds::AO_NO_LONGER_AO, 'AO no longer AO')
  end
  Npis::AO_FAILS_MED_CHECK.each do |npi|
    make_basic(npi, PacIds::AO_FAILS_MED_CHECK, 'AO Fails Med')
  end
  Npis::NO_AO.each do |npi|
    org = ProviderOrganization.find_or_create_by(npi:)
    org.name = "PERF Org No AO #{npi}"
    org.verification_status = 'approved'
    org.last_checked_at = 8.days.ago
    org.save!
  end
  PacIds::AO_HAS_TWO.each do |pac_id|
    make_multiple(pac_id, @good_npis, 2)
  end
  PacIds::AO_HAS_FOUR.each do |pac_id|
    make_multiple(pac_id, @good_npis, 4)
  end
  @good_npis.each do |npi|
    make_basic(npi, PacIds::AO_GOOD, 'Good')
  end
end

def make_multiple(pac_id, npis, count)
  raise unless pac_id

  user = User.find_or_create_by(pac_id:) do |u|
    u.email = "PERF-#{pac_id}@example.com"
    u.password = u.password_confirmation = Devise.friendly_token[0, 20]
  end
  user.update!(verification_status: 'approved')
  count.times do
    npi = npis.pop
    raise unless npi

    org = ProviderOrganization.find_or_create_by(npi:) do |o|
      o.name = "PERF: Sharing #{pac_id} #{npi}"
    end
    org.verification_status = 'approved'
    org.last_checked_at = 8.days.ago
    org.save!

    link = AoOrgLink.find_or_create_by(provider_organization: org, user:)
    link.verification_status = true
    link.last_checked_at = 8.days.ago
    link.save!
  end
end

def make_basic(npi, pac_ids, feature)
  raise unless npi

  org = ProviderOrganization.find_or_create_by(npi:)
  org.name = "PERF: #{feature} #{npi}"

  org.verification_status = 'approved'
  org.last_checked_at = 8.days.ago
  org.save!
  pac_id = pac_ids.pop
  raise unless pac_id

  user = User.find_or_create_by(pac_id:) do |u|
    u.email = "user#{pac_id}@example.com"
    u.password = u.password_confirmation = Devise.friendly_token[0, 20]
  end
  user.update!(verification_status: 'approved')

  link = AoOrgLink.find_or_create_by(provider_organization: org, user:)
  link.verification_status = true
  link.last_checked_at = 8.days.ago
  link.save!
end

def cleanup
  ProviderOrganization.where(npi: Npis::ORG_FAILS_MED_CHECK).each do |org|
    cleanup_org(org)
  end
  ProviderOrganization.where(npi: Npis::ORG_NO_ENROLLMENTS).each do |org|
    cleanup_org(org)
  end

  ProviderOrganization.where(npi: Npis::AO_NO_LONGER_AO).each do |org|
    cleanup_org(org)
  end
  ProviderOrganization.where(npi: Npis::AO_FAILS_MED_CHECK).each do |org|
    cleanup_org(org)
  end
  ProviderOrganization.where(npi: Npis::NO_AO).destroy_all
  User.where(pac_id: PacIds::AO_HAS_TWO).each do |user|
    cleanup_user(user)
  end
  User.where(pac_id: PacIds::AO_HAS_FOUR).each do |user|
    cleanup_user(user)
  end

  ProviderOrganization.where(npi: Npis::GOOD).each do |org|
    cleanup_org(org)
  end
end

def cleanup_user(user)
  links = AoOrgLink.where(user:)
  links.map(&:user)
  links.each do |link|
    link.destroy
    link.provider_organization.destroy
  end
  user.destroy
end

def cleanup_org(org)
  links = org.ao_org_links
  links.map(&:user)
  links.each do |link|
    link.destroy
    link.user.destroy
  end
  org.destroy
end

# Fakes an org necessary to work with the DpcClient
class MockOrg
  # rubocop:disable Naming/VariableNumber
  attr_reader :npi, :name, :address_use, :address_type, :address_city, :address_state, :address_street,
              :address_street_2, :address_zip

  def initialize
    randy = Random.new
    @name = "Generated Organization #{randy.rand(1000)}"
    @npi = Luhnacy.doctor_npi[-10..]
    @address_use = 'work'
    @address_type = 'both'
    @address_street = "#{randy.rand(1000)} Elm St"
    @address_street_2 = "Suite #{randy.rand(100)}"
    @address_city = 'Akron'
    @address_state = 'OH'
    @address_zip = '22222'
  end
  # rubocop:enable Naming/VariableNumber
end

class BadEnvironmentError < StandardError; end
