# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::CredentialDelegate::NewInvitationComponent, type: :system, js: true do
  include Devise::Test::IntegrationHelpers
  include DpcClientSupport

  before do
    driven_by(:selenium_headless)
  end

  let(:dpc_api_organization_id) { 'some-gnarly-guid' }

  context 'CD invite' do
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization, dpc_api_organization_id:, name: 'Health Hut') }
    let!(:ao_org_link) { create(:ao_org_link, user:, provider_organization: org) }

    before do
      sign_in user
      org.update!(terms_of_service_accepted_by: user)
    end

    it 'shows the ack modal on send invite button click' do
      visit "/organizations/#{org.id}/credential_delegate_invitations/new"
      expect(page).to have_selector('#verify-modal', visible: false)
      fill_in 'First or given name', with: 'John'
      fill_in 'Last or family name', with: 'Smith'
      fill_in 'Email', with: 'fake@fake.com'
      fill_in 'Confirm email', with: 'fake@fake.com'
      click_button 'Send invite'
      expect(page).to have_selector('#verify-modal', visible: true)
    end
  end
end
