# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Invitations::InvitationLoginComponent, type: :component do
  include ComponentSupport
  describe 'login component' do
    let(:provider_organization) { build(:provider_organization, dpc_api_organization_id: 'foo') }
    let(:invitation) { create(:invitation, :cd, provider_organization:) }
    let(:component) { described_class.new(invitation) }
    before { render_inline(component) }
    it 'should have a login button' do
      expect(page).to have_selector('button.usa-button')
      expect(page.find('button.usa-button')).to have_content('Sign in with')
      expect(page).to have_selector('button.usa-button span.login-button__logo')
      expect(page.find('button.usa-button span.login-button__logo')).to have_content('Login.gov')
    end

    it 'should render a link to the System Use Agreement' do
      expect(page).to have_link('System Use Agreement',
                                href: Rails.application.routes.url_helpers.system_use_agreement_path)
    end

    it 'should post to appropriate url' do
      path = "organizations/#{provider_organization.id}/invitations/#{invitation.id}/login"
      url = "http://test.host/portal/#{path}"
      expect(page.find('form')[:action]).to eq url
      expect(page.find('form')[:method]).to eq 'post'
    end
  end

  describe 'ao' do
    let(:invitation) { create(:invitation, :ao) }
    let(:component) { described_class.new(invitation) }
    before { render_inline(component) }
    it 'should have step component at step 2' do
      expect(page).to have_selector('.usa-step-indicator__current-step')
      expect(page.find('.usa-step-indicator__current-step').text).to eq '2'
    end
  end

  describe 'cd' do
    let(:invitation) { create(:invitation, :cd) }
    let(:component) { described_class.new(invitation) }
    before { render_inline(component) }
    it 'should have step component at step 2' do
      expect(page).to have_selector('.usa-step-indicator__current-step')
      expect(page.find('.usa-step-indicator__current-step').text).to eq '2'
    end
  end
end
