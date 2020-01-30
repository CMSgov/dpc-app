# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'creating and updating organizations' do
  include APIClientSupport

  let!(:internal_user) { create :internal_user }

  before(:each) do
    sign_in internal_user, scope: :internal_user
  end

  scenario 'successfully creating and updating an organization\'s attributes' do
    visit new_internal_organization_path

    fill_in 'organization_name', with: 'Good Health'
    select 'Primary Care Clinic', from: 'organization_organization_type'
    fill_in 'organization_num_providers', with: '2200'

    select 'Temp', from: 'organization_address_attributes_address_use'
    select 'Both', from: 'organization_address_attributes_address_type'
    fill_in 'organization_address_attributes_street', with: '1 North Main'
    fill_in 'organization_address_attributes_street_2', with: 'Ste 2000'
    fill_in 'organization_address_attributes_city', with: 'Greenville'
    select 'South Carolina', from: 'organization_address_attributes_state'
    fill_in 'organization_address_attributes_zip', with: '29601'

    fill_in 'organization_vendor', with: 'Cool EMR Vendor'
    fill_in 'organization_npi', with: '555ttt444'

    find('[data-test="form-submit"]').click

    expect(page).not_to have_css('[data-test="form-submit"]')

    expect(page.body).to have_content('Good Health')
    expect(page.body).to have_content('2200')
    expect(page.body).to have_content('Primary Care Clinic')
    expect(page.body).to have_content('1 North Main')
    expect(page.body).to have_content('Cool EMR Vendor')

    find('[data-test="edit-link"]').click

    fill_in 'organization_name', with: 'Health Revisited'
    fill_in 'organization_npi', with: '9987966711'
    select 'Multispecialty Clinic', from: 'organization_organization_type'
    fill_in 'organization_address_attributes_street', with: '50 River St'
    find('[data-test="form-submit"]').click

    expect(page).not_to have_css('[data-test="form-submit"]')
    expect(page.body).to have_content('Health Revisited')
    expect(page.body).to have_content('9987966711')
    expect(page.body).to have_content('Multispecialty Clinic')
    expect(page.body).to have_content('50 River St')
  end

  scenario 'trying to update an organization with invalid attributes ' do
    org = create(:organization, name: 'Good Health')

    visit edit_internal_organization_path(org)
    expect(page.body).to have_content('Good Health')

    fill_in 'organization_name', with: ''
    find('[data-test="form-submit"]').click

    # Still on edit page
    expect(page).to have_css('[data-test="form-submit"]')
  end

  scenario 'enabling sandbox access successfully' do
    stub_api_client(
      message: :create_organization,
      success: true,
      response: default_org_creation_response
    )

    org = create(:organization)
    crabby = create(:user, first_name: 'Crab', last_name: 'Olsen', email: 'co@beach.com')
    fishy = create(:user, first_name: 'Fish', last_name: 'Marlin', email: 'fish@beach.com')
    create(:user, first_name: 'Unrelated', last_name: 'User', email: 'unrelated@beach.com')
    org.users << crabby
    org.users << fishy

    mailer = stub_sandbox_notification_mailer(org, [crabby, fishy])

    visit internal_organization_path(org)
    find('[data-test="enable-sandbox"]').click

    expect(page).to have_css('[data-test="new-reg-org-sandbox"]')
    fill_in 'registered_organization_fhir_endpoint_attributes_name', with: 'Test Sandbox Endpoint'
    select 'Test', from: 'registered_organization_fhir_endpoint_attributes_status'
    fill_in 'registered_organization_fhir_endpoint_attributes_uri', with: 'https://example.com'
    find('[data-test="form-submit"]').click

    expect(page).to have_content('Access to sandbox enabled.')
    expect(mailer).to have_received(:organization_sandbox_email).twice
  end

  scenario 'updating an API enabled organization' do
    stub_api_client(
      message: :create_organization,
      success: true,
      response: default_org_creation_response
    )

    org = create(:organization, :sandbox_enabled)
    reg_org = org.sandbox_registered_organization

    visit internal_organization_path(org)
    find('[data-test="edit-link"]').click

    new_name = 'Updated Organization Name'
    api_client = stub_api_client(
      message: :update_organization,
      success: true,
      response: default_org_creation_response
    )

    fill_in 'organization_name', with: new_name
    find('[data-test="form-submit"]').click

    expect(api_client).to have_received(:update_organization).with(reg_org)
  end

  scenario 'disabling sandbox access successfully' do
    # TODO with separate #delete_organization work
  end

  def stub_sandbox_notification_mailer(org, users=[])
    mailer = double(UserMailer)
    users.each do |user|
      allow(UserMailer).to receive(:with).with(user: user, organization: org).and_return(mailer)
    end

    allow(mailer).to receive(:organization_sandbox_email).and_return(mailer)
    allow(mailer).to receive(:deliver_later)
    mailer
  end
end
